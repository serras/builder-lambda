package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

object ClassAnchorElementKey : FirDeclarationDataKey()

var FirClass.anchor: KtSourceElement? by FirDeclarationDataRegistry.data(ClassAnchorElementKey)
val FirClassSymbol<*>.anchor: KtSourceElement? by FirDeclarationDataRegistry.symbolAccessor(ClassAnchorElementKey)

object GeneratedClassElementKey : FirDeclarationDataKey()

var FirClass.generatedCallData: GeneratedCallData? by FirDeclarationDataRegistry.data(GeneratedClassElementKey)
val FirClassSymbol<*>.generatedCallData: GeneratedCallData? by FirDeclarationDataRegistry.symbolAccessor(GeneratedClassElementKey)

class CallDataStorage(session: FirSession) : FirExtensionSessionComponent(session) {
    val generatedCallData =
        session.firCachesFactory.createCache<FirNamedFunctionSymbol, GeneratedCallData, GeneratedCallData?> { symbol, context ->
            context ?: error("context not provided for $symbol")
        }
}

val FirSession.callDataStorage: CallDataStorage by FirSession.sessionComponentAccessor()

class GeneratedCallData(
    val classToBuild: FirClassSymbol<*>,
    val builderClass: FirClassSymbol<*>,
    val localBuilderClass: FirRegularClass,
)