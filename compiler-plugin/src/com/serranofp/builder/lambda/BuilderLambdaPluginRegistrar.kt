package com.serranofp.builder.lambda

import com.serranofp.builder.lambda.fir.MembersGenerator
import com.serranofp.builder.lambda.fir.CallRefinement
import com.serranofp.builder.lambda.fir.CallDataStorage
import com.serranofp.builder.lambda.fir.Checker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(FirExtensionApiInternals::class)
class BuilderLambdaPluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CallDataStorage
        +::CallRefinement
        +::MembersGenerator
        +::BuilderLambdaFirCheckers
    }
}

class BuilderLambdaFirCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers
        get() = object : ExpressionCheckers() {
            override val functionCallCheckers: Set<FirFunctionCallChecker>
                get() = setOf(Checker)
        }
}
