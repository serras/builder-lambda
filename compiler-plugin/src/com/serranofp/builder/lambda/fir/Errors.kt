package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object Errors : KtDiagnosticsContainer() {
    val NOT_A_BUILDER by error0<KtElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("Builder lambda errors") {
            it.put(NOT_A_BUILDER, "This type does not follow the Builder pattern")
        }
    }
}