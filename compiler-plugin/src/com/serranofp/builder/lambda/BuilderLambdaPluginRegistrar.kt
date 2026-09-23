package com.serranofp.builder.lambda

import com.serranofp.builder.lambda.fir.BuilderLambdaMembersGenerator
import com.serranofp.builder.lambda.fir.BuilderLambdaRefinementExtension
import com.serranofp.builder.lambda.fir.CallDataStorage
import org.jetbrains.kotlin.compiler.plugin.template.fir.SimpleClassGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@OptIn(FirExtensionApiInternals::class)
class BuilderLambdaPluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CallDataStorage
        +::BuilderLambdaRefinementExtension
        +::BuilderLambdaMembersGenerator
    }
}
