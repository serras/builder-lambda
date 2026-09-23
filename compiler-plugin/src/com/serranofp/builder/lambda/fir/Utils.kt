package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.declaredFunctions
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun FirClassSymbol<*>.builderFunction(session: FirSession): FirNamedFunctionSymbol? =
    declaredFunctions(session).singleOrNull {
        it.name == Name.identifier("builder") && it.isStatic
    }

internal fun FirClassSymbol<*>.builderBuildFunction(session: FirSession): FirNamedFunctionSymbol? =
    declaredFunctions(session).singleOrNull {
        it.name == Name.identifier("build") && !it.isStatic && it.valueParameterSymbols.isEmpty()
    }

internal fun FirExpression?.builderClass(session: FirSession): FirClassSymbol<*>? =
    (this as? FirFunctionCall)
        ?.takeIf { (calleeReference.symbol as? FirCallableSymbol<*>)?.callableId == BuilderLambdaIds.BUILDER_FUNCTION_ID }
        ?.typeArguments?.singleOrNull()?.builderClass(session)

internal fun FirTypeProjection.builderClass(session: FirSession): FirClassSymbol<*>? =
    toConeTypeProjection().type?.toClassSymbol(session)
        ?.builderFunction(session)?.resolvedReturnType?.toClassSymbol(session)

internal fun localClassId(name: Name) =
    ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName.ROOT.child(name), isLocal = true)

internal fun findLet(session: FirSession): FirFunctionSymbol<*> {
    return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).single()
}