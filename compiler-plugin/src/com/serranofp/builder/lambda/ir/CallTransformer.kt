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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

val BUILDER_LAMBDA_INITIAL_CALL by IrStatementOriginImpl
val BUILDER_LAMBDA_ACCUMULATION_CALL by IrStatementOriginImpl
val BUILDER_LAMBDA_FINAL_CALL by IrStatementOriginImpl
val BUILDER_LAMBDA_RUN by IrStatementOriginImpl
val BUILDER_LAMBDA_VAR by IrDeclarationOriginImpl.Synthetic

class CallTransformerExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(CallTransformer())
    }
}

class CallTransformer : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        (val runArgument, val localBuilderClass, val typeToBuild, val buildBody) = expression.obtainArguments() ?: return super.visitCall(expression)
        val runBody = runArgument.body!!
        val functionsAndSetters = localBuilderClass.functionsAndSetters().map { it.symbol }

        val builderStatic = typeToBuild.declarations.find { it is IrFunction && it.isStatic && it.name == Name.identifier("builder") } as IrSimpleFunction
        val builderClass = (builderStatic.returnType as IrSimpleType).classifier.owner as IrClass
        val builderConstructor = builderClass.constructors.singleOrNull() ?: builderStatic
        val builderBuild = builderClass.declarations.find { it is IrFunction && it.name == Name.identifier("build") } as IrSimpleFunction

        val newBody = IrBlockImpl(
            startOffset = expression.startOffset, endOffset = expression.endOffset,
            type = expression.type, origin = BUILDER_LAMBDA_RUN
        )

        val theVariableSymbol = IrVariableSymbolImpl()
        val theVariable = IrVariableImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_VAR,
            symbol = theVariableSymbol, name = Name.special("<builder>"), type = builderConstructor.returnType,
            isVar = true, isConst = false, isLateinit = false
        ).apply { parent = runArgument.parent }
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

        val construct = when (builderConstructor) {
            is IrConstructor -> IrConstructorCallImpl(
                startOffset = runBody.startOffset, endOffset = runBody.endOffset,
                type = builderConstructor.returnType, symbol = builderConstructor.symbol,
                typeArgumentsCount = 0, constructorTypeArgumentsCount = 0
            ).apply {
                arguments.clear()
                for (parameter in builderParameters) {
                    arguments.add(requiredArguments[parameter]!!)
                }
            }
            is IrSimpleFunction -> IrCallImpl(
                startOffset = runBody.startOffset, endOffset = runBody.endOffset,
                type = builderConstructor.returnType, symbol = builderConstructor.symbol,
            )
        }

        newBody.statements += IrSetValueImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_INITIAL_CALL,
            type = builderConstructor.returnType, symbol = theVariableSymbol,
            value = construct
        )

        while (iterator.hasNext()) {
            val statement = iterator.next()

            if (statement !is IrCall || statement.symbol !in functionsAndSetters) {
                newBody.statements += statement
                continue
            }

            val symbol = statement.symbol
            val name = when (val property = symbol.owner.correspondingPropertySymbol) {
                null -> symbol.owner.name
                else -> property.owner.name // it is a setter
            }
            val argumentsToCall = statement.arguments.drop(1).filterNotNull() // drop dispatch
            val corresponding = builderClass.findCorresponding(name, symbol.owner.parameters.drop(1).map { it.type })
            newBody.statements += IrSetValueImpl(
                startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_ACCUMULATION_CALL,
                type = corresponding.returnType, symbol = theVariableSymbol,
                value = IrCallImpl(
                    startOffset = runBody.startOffset, endOffset = runBody.endOffset,
                    type = corresponding.returnType, symbol = corresponding.symbol,
                ).apply {
                    arguments.clear()
                    arguments.add(IrGetValueImpl(
                        startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_ACCUMULATION_CALL,
                        type = builderConstructor.returnType, symbol = theVariableSymbol
                    ))
                    arguments.addAll(argumentsToCall)
                }
            )
        }

        val lastCall = IrCallImpl(
            startOffset = runBody.startOffset, endOffset = runBody.endOffset,
            type = builderBuild.returnType, symbol = builderBuild.symbol
        ).apply {
            arguments.clear()
            arguments.add(IrGetValueImpl(
                startOffset = runBody.startOffset, endOffset = runBody.endOffset, origin = BUILDER_LAMBDA_FINAL_CALL,
                type = builderConstructor.returnType, symbol = theVariableSymbol
            ))
        }

        newBody.statements += lastCall
        return newBody
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

    fun IrClass.findCorresponding(name: Name, parameterTypes: List<IrType>): IrSimpleFunction {
        val functions = declarations.filterIsInstance<IrSimpleFunction>().filter {
            it.parameters.drop(1).map { it.type } == parameterTypes
        }
        return functions.firstOrNull { it.name == name } ?: functions.first { compatibleSetOptName(it.name, name) }
    }

    fun compatibleSetOptName(fnName: Name, builderName: Name): Boolean {
        val fnNameS = fnName.asString()
        val builderNameS = builderName.asString().capitalizeAsciiOnly()
        return fnNameS == "set$builderNameS" || fnNameS == "opt$builderNameS"
    }
}