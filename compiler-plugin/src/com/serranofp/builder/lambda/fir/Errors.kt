package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.COLLECTION
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.DECLARATION_NAME
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.psi.KtElement

object Errors : KtDiagnosticsContainer() {
    val NOT_A_BUILDER_BUILDER by error0<KtElement>()
    val NOT_A_BUILDER_CONSTRUCTOR by error1<KtElement, FirClassSymbol<*>>()
    val NOT_A_BUILDER_NO_BUILD by error1<KtElement, FirClassSymbol<*>>()
    val MUST_USE_LAMBDA by error0<KtElement>()
    val CONSTRUCTOR_ARG_GO_FIRST by error0<KtElement>()
    val CONSTRUCTOR_ARGS_MISSING by error1<KtElement, List<FirValueParameterSymbol>>()
    val BUILDER_CANNOT_BE_READ by error0<KtElement>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    const val NOT_A_BUILDER_MSG = "The type does not follow the Builder pattern:"

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("Builder lambda errors") {
            it.put(NOT_A_BUILDER_BUILDER, "$NOT_A_BUILDER_MSG missing ''builder'' function or ''Builder'' type")
            it.put(NOT_A_BUILDER_CONSTRUCTOR, "$NOT_A_BUILDER_MSG ''{0}'' must have a single constructor", DECLARATION_NAME)
            it.put(NOT_A_BUILDER_NO_BUILD, "$NOT_A_BUILDER_MSG ''{0}'' has no (correct) ''build'' function", DECLARATION_NAME)
            it.put(MUST_USE_LAMBDA, "The argument to ''build'' must be a lambda")
            it.put(CONSTRUCTOR_ARG_GO_FIRST, "Required builder argument, it must be assigned first")
            it.put(CONSTRUCTOR_ARGS_MISSING, "Required builder arguments missing: {0}", COLLECTION(DECLARATION_NAME))
            it.put(BUILDER_CANNOT_BE_READ, "Builder arguments cannot be read from")
        }
    }
}