package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object Errors : KtDiagnosticsContainer() {
    val NOT_A_BUILDER by error0<KtElement>()
    // val LONELY_BUILDER by error0<KtElement>()
    val WITH_MUST_USE_BUILD by error0<KtElement>()
    val WITH_MUST_USE_LAMBDA by error0<KtElement>()
    val CONSTRUCTOR_ARG_GO_FIRST by error0<KtElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("Builder lambda errors") {
            it.put(NOT_A_BUILDER, "This type does not follow the Builder pattern")
            // it.put(LONELY_BUILDER, "The call to ''build'' must be follow by a call to ''with'', as in ''build<T>().with { ... }''")
            it.put(WITH_MUST_USE_BUILD, "The call to ''with'' must follow the call to ''build'', as in ''build<T>().with { ... }''")
            it.put(WITH_MUST_USE_LAMBDA, "Only lambdas are supported")
            it.put(CONSTRUCTOR_ARG_GO_FIRST, "Required builder argument, it must be assigned first")
        }
    }
}