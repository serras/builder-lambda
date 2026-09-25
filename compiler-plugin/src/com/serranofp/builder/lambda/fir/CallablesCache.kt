package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class CallablesCache(val session: FirSession) {
    private val callablesCache: FirCache<FirClassSymbol<*>, Map<Name, List<Category>>?, Nothing?> =
        session.firCachesFactory.createCache { k, _ -> k.computeCallables(includeConstructors = true) }

    operator fun get(symbol: FirClassSymbol<*>): Map<Name, List<Category>>? = callablesCache.getValue(symbol)

    @OptIn(ExperimentalStdlibApi::class)
    fun FirClassSymbol<*>.computeCallables(
        includeConstructors: Boolean,
        storage: MutableMap<Name, MutableList<Category>> = mutableMapOf(),
    ): Map<Name, List<Category>> {
        val scope = unsubstitutedScope(session, ScopeSession(), true, null)
        if (includeConstructors) {
            scope.getDeclaredConstructors().singleOrNull()?.let { constructor ->
                for (parameter in constructor.valueParameterSymbols) {
                    storage.getOrPutIfMissing(parameter.name, { mutableListOf() }).add(Category.Constructor(parameter))
                }
            }
        }
        scope.processAllCallables processor@{ callable ->
            when {
                callable.resolvedAndJavaizedReturnTypeSymbol(session)?.classId != this.classId -> {}
                callable !is FirFunctionSymbol<*> -> {}
                callable.valueParameterSymbols.any { it.isVararg } -> {}
                // do not add 'clearXX' things
                callable.name.asString().startsWith("clear") && callable.valueParameterSymbols.isEmpty() -> {}
                callable.valueParameterSymbols.size != 1 -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Function(callable))
                }
                scope.hasSameNameFor(callable) -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Function(callable))
                }
                scope.hasPluralFor(callable.name) -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Singular(callable))
                }
                else -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Property(callable))
                }
            }
        }
        // getSuperTypes(session).forEach { superType ->
        //     superType.toClassSymbol(session)?.computeCallables(includeConstructors = false, storage)
        // }
        return storage
    }

    fun FirTypeScope.hasSameNameFor(symbol: FirCallableSymbol<*>): Boolean {
        var found = false
        processFunctionsByName(symbol.name) processor@{
            if (found) return@processor
            if (it != symbol) { found = true }
        }
        return found
    }

    fun FirTypeScope.hasPluralFor(name: Name): Boolean {
        if (name.isSpecial) return false
        val singularName = name.asString()
        var found = false
        processAllCallables processor@{
            if (found || it.name.isSpecial) return@processor
            val thisName = it.name.asString()
            if (Singulars.from(thisName) == singularName) {
                found = true
            }
        }
        return found
    }

    fun getConstructorParameters(symbol: FirClassSymbol<*>): List<FirValueParameterSymbol> =
        get(symbol).orEmpty().flatMap { [_, categories] ->
            categories.mapNotNull { if (it is Category.Constructor) it.symbol else null }
        }
}

sealed interface Category {
    val symbol: FirCallableSymbol<*>

    sealed interface CreatesProperty : Category
    data class Property(override val symbol: FirFunctionSymbol<*>) : CreatesProperty
    data class Constructor(override val symbol: FirValueParameterSymbol) : CreatesProperty

    sealed interface CreatesFunction : Category {
        override val symbol: FirFunctionSymbol<*>
    }
    data class Singular(override val symbol: FirFunctionSymbol<*>) : CreatesFunction
    data class Function(override val symbol: FirFunctionSymbol<*>) : CreatesFunction
}