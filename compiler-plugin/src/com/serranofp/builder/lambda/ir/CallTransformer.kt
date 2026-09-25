package com.serranofp.builder.lambda.ir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val BUILDER_LAMBDA_INITIAL_CALL by IrStatementOriginImpl
val BUILDER_LAMBDA_FINAL_CALL by IrStatementOriginImpl
val BUILDER_LAMBDA_VAR by IrDeclarationOriginImpl.Synthetic

class CallTransformerExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(CallTransformer(pluginContext.irFactory))
    }
}

class CallTransformer(val factory: IrFactory) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        (val runArgument, val localBuilderClass, val typeToBuild, val buildBody) = expression.obtainArguments() ?: return super.visitCall(expression)
        val runBody = runArgument.body!!
        val functionsAndSetters = localBuilderClass.functionsAndSetters().map { it.symbol }

        val builderStatic = typeToBuild.declarations.find { it is IrFunction && it.isStatic && it.name == Name.identifier("builder") } as IrSimpleFunction
        val builderClass = (builderStatic.returnType as IrSimpleType).classifier.owner as IrClass
        val builderConstructor = builderClass.constructors.single()
        val builderBuild = builderClass.declarations.find { it is IrFunction && it.name == Name.identifier("build") } as IrSimpleFunction

        val newBody = factory.createBlockBody(startOffset = runBody.startOffset, endOffset = runBody.endOffset)

        val theVariableSymbol = IrVariableSymbolImpl()
        val theVariable = IrVariableImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_VAR,
            symbol = theVariableSymbol, name = Name.special("<builder>"), type = builderConstructor.returnType,
            isVar = true, isConst = false, isLateinit = false
        ).apply { parent = runArgument }
        newBody.statements += theVariable

        val iterator = buildBody.iterator()

        val builderParameters = builderConstructor.parameters
        val requiredArguments = mutableMapOf<IrValueParameter, IrExpression>()
        while (requiredArguments.size < builderParameters.size && iterator.hasNext()) {
            val statement = iterator.next()

            if (statement !is IrCall || statement.symbol !in functionsAndSetters) {
                newBody.statements += statement
            } else {
                val name = statement.symbol.owner.correspondingPropertySymbol?.owner?.name
                val parameter = builderParameters.single { it.name == name }
                requiredArguments[parameter] = statement.arguments[1]!!
            }
        }

        newBody.statements += IrSetValueImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_INITIAL_CALL,
            type = builderConstructor.returnType, symbol = theVariableSymbol,
            value = IrConstructorCallImpl(
                startOffset = runBody.startOffset, endOffset = runBody.endOffset,
                type = builderConstructor.returnType, symbol = builderConstructor.symbol,
                typeArgumentsCount = 0, constructorTypeArgumentsCount = 0
            ).apply {
                arguments.clear()
                for (parameter in builderParameters) {
                    arguments.add(requiredArguments[parameter]!!)
                }
            }
        )

        while (iterator.hasNext() && requiredArguments.size < builderConstructor.parameters.size) {
            val statement = iterator.next()

            if (statement !is IrCall || statement.symbol !in functionsAndSetters) {
                newBody.statements += statement
                continue
            }
        }

        val lastCall = IrCallImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset,
            type = builderBuild.returnType, symbol = builderBuild.symbol
        ).apply {
            dispatchReceiver = IrGetValueImpl(
                startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_FINAL_CALL,
                type = builderConstructor.returnType, symbol = theVariableSymbol
            )
        }
        newBody.statements += IrReturnImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset,
            type = builderBuild.returnType, value = lastCall, returnTargetSymbol = runArgument.symbol
        )

        runArgument.body = newBody
        return expression
    }

    data class BuildCallInfo(val runArgument: IrSimpleFunction, val localBuilderClass: IrClass, val typeToBuild: IrClass, val buildBody: List<IrStatement>)

    fun IrCall.obtainArguments(): BuildCallInfo? {
        if (symbol.fqNameWhenAvailable != FqName.fromSegments(listOf("kotlin", "run"))) return null
        val runArgument = (arguments.singleOrNull() as? IrFunctionExpression)?.function ?: return null
        val runBody = runArgument.body?.statements ?: return null
        if (runBody.size != 2) return null
        val [localBuilderClass, returnCall] = runBody
        if (localBuilderClass !is IrClass || returnCall !is IrReturn) return null
        val call = returnCall.value as? IrCall ?: return null
        if (call.symbol.fqNameWhenAvailable != BuilderLambdaIds.BUILD_FUNCTION_FQNAME) return null
        val typeToBuild = (call.typeArguments.firstOrNull() as? IrSimpleType)?.classifier?.owner as? IrClass ?: return null
        val buildLambda = call.arguments.singleOrNull() as? IrFunctionExpression ?: return null
        val buildBody = buildLambda.function.body?.statements ?: return null
        return BuildCallInfo(runArgument, localBuilderClass, typeToBuild, buildBody)
    }

    fun IrClass.functionsAndSetters(): List<IrSimpleFunction> =
        declarations.filterIsInstance<IrSimpleFunction>() + declarations.filterIsInstance<IrProperty>().mapNotNull { it.setter }
}