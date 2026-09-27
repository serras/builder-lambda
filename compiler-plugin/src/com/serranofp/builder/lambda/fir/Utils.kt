package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.processAllClassifiers
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun FirClassSymbol<*>.builderFunction(session: FirSession): FirNamedFunctionSymbol? {
    var symbol: FirNamedFunctionSymbol? = null
    val staticScope = this.staticScope(session, ScopeSession())
    staticScope?.processFunctionsByName(Name.identifier("builder")) {
        if (it.isStatic) { symbol = it }
    }
    if (symbol == null) {
        // try to find in companion object
        staticScope?.processAllClassifiers { companion ->
            if (companion is FirClassSymbol<*> && companion.isCompanion) {
                companion.unsubstitutedScope(session, ScopeSession(), true, null)
                    .processFunctionsByName(Name.identifier("builder")) { symbol = it }
            }
        }
    }
    return symbol
}

internal fun FirClassSymbol<*>.builderBuildFunction(session: FirSession): FirNamedFunctionSymbol? {
    var symbol: FirNamedFunctionSymbol? = null
    this.unsubstitutedScope(session, ScopeSession(), true, null).processFunctionsByName(Name.identifier("build")) {
        if (!it.isStatic && it.valueParameterSymbols.isEmpty()) { symbol = it }
    }
    return symbol
}

internal fun FirExpression?.builderClass(session: FirSession): FirClassSymbol<*>? =
    (this as? FirFunctionCall)
        ?.takeIf { (calleeReference.symbol as? FirCallableSymbol<*>)?.callableId == BuilderLambdaIds.BUILD_FUNCTION_ID }
        ?.typeArguments?.singleOrNull()?.builderClass(session)

internal fun FirTypeProjection.builderClass(session: FirSession): FirClassSymbol<*>? =
    toConeTypeProjection().type?.toClassSymbol(session)
        ?.builderFunction(session)?.resolvedAndJavaizedReturnTypeSymbol(session)

internal fun localClassId(name: Name) =
    ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName.ROOT.child(name), isLocal = true)

internal fun findLet(session: FirSession): FirFunctionSymbol<*> {
    return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).single()
}

internal fun findRun(session: FirSession): FirFunctionSymbol<*> {
    return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("run"))
        .first { it.typeParameterSymbols.size == 1 }
}

@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
internal fun FirCallableSymbol<*>.resolvedAndJavaizedReturnTypeSymbol(session: FirSession): FirClassSymbol<*>? =
    when (val r = fir.returnTypeRef) {
        is FirJavaTypeRef -> {
            val javaClass = ((r.type as? JavaClassifierType)?.classifier as? JavaClass)
            val symbol = javaClass?.classId?.let { session.javaSymbolProvider?.getClassLikeSymbolByClassId(it) }
            return symbol
        }
        else -> r.toRegularClassSymbol(session)
    }