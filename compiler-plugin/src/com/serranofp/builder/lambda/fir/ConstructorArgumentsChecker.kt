package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object ConstructorArgumentsChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        // check we are in the right place
        val symbol = expression.calleeReference.symbol as? FirCallableSymbol<*> ?: return
        if (symbol.callableId != BuilderLambdaIds.WITH_FUNCTION_ID) return

        val receiver = expression.extensionReceiver
        if (receiver !is FirFunctionCall || (receiver.calleeReference.symbol as? FirCallableSymbol<*>)?.callableId != BuilderLambdaIds.BUILDER_FUNCTION_ID) {
            reporter.reportOn(receiver?.source ?: expression.source, Errors.WITH_MUST_USE_BUILD)
        }

        val argument = expression.argument
        if (argument !is FirAnonymousFunctionExpression) {
            reporter.reportOn(argument.source, Errors.WITH_MUST_USE_LAMBDA)
            return
        }

        val builderClass = receiver.builderClass(context.session) ?: return

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