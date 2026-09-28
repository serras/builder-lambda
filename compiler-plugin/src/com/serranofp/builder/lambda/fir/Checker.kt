package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object Checker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        // check we are in the right place
        val symbol = expression.calleeReference.symbol as? FirCallableSymbol<*> ?: return
        if (symbol.callableId != BuilderLambdaIds.BUILD_FUNCTION_ID) return
        val typeArgument = expression.typeArguments.firstOrNull() ?: return

        // check we follow the Builder pattern
        val typeToBuild =  typeArgument.toConeTypeProjection().type
        val builderClass = typeArgument.builderClass(context.session)
        val builderClassConstructor = builderClass?.constructors(context.session)?.singleOrNull {
            it.origin !is FirDeclarationOrigin.Synthetic
        }
        val builderBuildFunction = builderClass?.builderBuildFunction(context.session)

        val followsBuilderPattern =
            builderClassConstructor != null && builderBuildFunction != null &&
                    typeToBuild != null && builderBuildFunction.resolvedAndJavaizedReturnTypeSymbol(context.session)?.classId == typeToBuild.classId

        if (!followsBuilderPattern) {
            when {
                builderClass == null -> reporter.reportOn(typeArgument.source, Errors.NOT_A_BUILDER_BUILDER)
                builderClassConstructor == null -> reporter.reportOn(typeArgument.source, Errors.NOT_A_BUILDER_CONSTRUCTOR, builderClass)
                else -> reporter.reportOn(typeArgument.source, Errors.NOT_A_BUILDER_NO_BUILD, builderClass)
            }
        }

        val argument = expression.argument
        if (argument !is FirAnonymousFunctionExpression) {
            reporter.reportOn(argument.source, Errors.MUST_USE_LAMBDA)
        }

        if (!followsBuilderPattern || argument !is FirAnonymousFunctionExpression) return

        val cache = CallablesCache(context.session)
        val localBuilderClass = expression.typeArguments[1].toConeTypeProjection().type ?: return

        val body = argument.anonymousFunction.body ?: return
        val requiredNames = cache.getRequiredNames(builderClass)
        var afterRequiredNames = false
        val givenRequiredNames = mutableSetOf<Name>()
        for (statement in body.statements) {
            val requiredName = statement.extractAssignmentToRequired(localBuilderClass, requiredNames)

            if (!afterRequiredNames) {
                if (requiredName != null) { givenRequiredNames.add(requiredName) }
                else { afterRequiredNames = true }
            } else if (requiredName != null) {
                reporter.reportOn(statement.lValue.source, Errors.CONSTRUCTOR_ARG_GO_FIRST)
            }
        }

        val missingParameters = requiredNames - givenRequiredNames
        if (missingParameters.isNotEmpty()) {
            reporter.reportOn(expression.calleeReference.source, Errors.CONSTRUCTOR_ARGS_MISSING, missingParameters)
        }

        body.accept(object : FirVisitor<Unit, Boolean>() {
            // 'data' tells us whether we should check the property
            override fun visitElement(element: FirElement, data: Boolean) {
                when (element) {
                    // on assignments we allow one level of property
                    is FirVariableAssignment -> {
                        element.lValue.accept(this, false)
                        element.rValue.accept(this, true)
                    }
                    is FirAugmentedAssignment -> {
                        element.leftArgument.accept(this, false)
                        element.rightArgument.accept(this, true)
                    }
                    is FirPropertyAccessExpression if data -> {
                        val dispatch = element.dispatchReceiver as? FirThisReceiverExpression
                        if (dispatch?.resolvedType?.classId == localBuilderClass.classId) {
                            reporter.reportOn(element.source, Errors.BUILDER_CANNOT_BE_READ)
                        }
                        element.acceptChildren(this, true)
                    }
                    else -> element.acceptChildren(this, true)
                }
            }
        }, true)
    }

    @OptIn(ExperimentalContracts::class)
    private fun FirStatement.extractAssignmentToRequired(receiver: ConeKotlinType, requiredNames: Set<Name>): Name? {
        contract {
            returnsNotNull() implies (this@extractAssignmentToRequired is FirVariableAssignment)
        }

        if (this !is FirVariableAssignment) return null
        val lValue = lValue as? FirPropertyAccessExpression ?: return null
        val dispatch = lValue.dispatchReceiver as? FirThisReceiverExpression ?: return null
        if (dispatch.resolvedType.classId != receiver.classId) return null
        return lValue.calleeReference.name.takeIf { it in requiredNames }
    }

}