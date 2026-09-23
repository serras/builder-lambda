package com.serranofp.builder.lambda

import com.serranofp.builder.lambda.fir.BuilderLambdaMembersGenerator
import com.serranofp.builder.lambda.fir.BuilderLambdaRefinementExtension
import com.serranofp.builder.lambda.fir.CallDataStorage
import com.serranofp.builder.lambda.fir.WrongBuilderLambdaFirChecker
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
        +::BuilderLambdaRefinementExtension
        +::BuilderLambdaMembersGenerator
        +::BuilderLambdaFirCheckers
    }
}

class BuilderLambdaFirCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers
        get() = object : ExpressionCheckers() {
            override val functionCallCheckers: Set<FirFunctionCallChecker>
                get() = setOf(WrongBuilderLambdaFirChecker())
        }
}
