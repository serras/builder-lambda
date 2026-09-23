package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type

class WrongBuilderLambdaFirChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        // check we are in the right place
        val symbol = expression.calleeReference.symbol as? FirCallableSymbol<*> ?: return
        if (symbol.callableId != BuilderLambdaIds.FUNCTION_ID) return
        val typeArgument = expression.typeArguments.singleOrNull() ?: return

        // check that the type follows the Builder pattern
        val typeToBuild =  typeArgument.toConeTypeProjection().type
        val classToBuild = typeToBuild?.toClassSymbol(context.session)
        val builderFunction = classToBuild?.builderFunction(context.session)
        val builderClass = builderFunction?.resolvedReturnType?.toClassSymbol(context.session)
        val builderBuildFunction = builderClass?.builderBuildFunction(context.session)

        if (builderBuildFunction == null) {
            reporter.reportOn(typeArgument.source, Errors.NOT_A_BUILDER)
        }
    }

}