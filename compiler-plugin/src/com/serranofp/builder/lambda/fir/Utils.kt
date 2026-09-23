package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.declaredFunctions
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun FirClassSymbol<*>.builderFunction(session: FirSession): FirNamedFunctionSymbol? =
    declaredFunctions(session).singleOrNull {
        it.name == Name.identifier("builder") && it.isStatic
    }

internal fun localClassId(name: Name) =
    ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName.ROOT.child(name), isLocal = true)

internal fun findLet(session: FirSession): FirFunctionSymbol<*> {
    return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).single()
}