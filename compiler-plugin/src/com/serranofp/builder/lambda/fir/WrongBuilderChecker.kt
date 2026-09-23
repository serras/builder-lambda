package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

object WrongBuilderLambdaFirChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        // check we are in the right place
        val symbol = expression.calleeReference.symbol as? FirCallableSymbol<*> ?: return
        if (symbol.callableId != BuilderLambdaIds.BUILDER_FUNCTION_ID) return
        val typeArgument = expression.typeArguments.singleOrNull() ?: return

        // check we follow the Builder pattern
        val builderClass = typeArgument.builderClass(context.session)
        val builderBuildFunction = builderClass?.builderBuildFunction(context.session)

        if (builderBuildFunction == null) {
            reporter.reportOn(typeArgument.source, Errors.NOT_A_BUILDER)
        }
    }

}