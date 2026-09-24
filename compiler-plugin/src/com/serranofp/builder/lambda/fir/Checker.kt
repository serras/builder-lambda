package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAugmentedAssignment
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
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
        val builderBuildFunction = builderClass?.builderBuildFunction(context.session)

        if (builderBuildFunction == null || typeToBuild == null || builderBuildFunction.resolvedReturnType.classId != typeToBuild.classId) {
            reporter.reportOn(typeArgument.source, Errors.NOT_A_BUILDER)
        }

        val argument = expression.argument
        if (argument !is FirAnonymousFunctionExpression) {
            reporter.reportOn(argument.source, Errors.MUST_USE_LAMBDA)
        }

        if (builderClass == null || argument !is FirAnonymousFunctionExpression) return

        val body = argument.anonymousFunction.body ?: return
        val cache = CallablesCache(context.session)
        var afterConstructorArguments = false
        for (statement in body.statements) {
            val isConstructorArgument = statement.isAssignmentToConstructor(cache, builderClass)

            if (!afterConstructorArguments && !isConstructorArgument) {
                afterConstructorArguments = true
            } else if (afterConstructorArguments && isConstructorArgument) {
                reporter.reportOn(statement.lValue.source, Errors.CONSTRUCTOR_ARG_GO_FIRST)
            }
        }

        val localBuilderClass = expression.typeArguments[1].toConeTypeProjection().type ?: return
        body.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                when (element) {
                    is FirVariableAssignment -> element.rValue.accept(this)
                    is FirAugmentedAssignment -> element.rightArgument.accept(this)
                    is FirPropertyAccessExpression -> {
                        val dispatch = element.dispatchReceiver as? FirThisReceiverExpression
                        if (dispatch?.resolvedType?.classId == localBuilderClass.classId) {
                            reporter.reportOn(element.source, Errors.BUILDER_CANNOT_BE_READ)
                        }
                        element.acceptChildren(this)
                    }
                    else -> element.acceptChildren(this)
                }
            }
        })
    }

    @OptIn(ExperimentalContracts::class)
    private fun FirStatement.isAssignmentToConstructor(cache: CallablesCache, receiver: FirClassSymbol<*>): Boolean {
        contract {
            returns(true) implies (this@isAssignmentToConstructor is FirVariableAssignment)
        }

        if (this !is FirVariableAssignment) return false
        val lValue = lValue as? FirPropertyAccessExpression ?: return false
        val dispatch = lValue.dispatchReceiver as? FirThisReceiverExpression ?: return false
        if (dispatch.resolvedType.classId != receiver.classId) return false
        return cache[receiver].orEmpty().any { [key, value] ->
            key == lValue.calleeReference.name && value.any { it is Category.Constructor }
        }
    }

}