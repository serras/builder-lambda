package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly

class CallablesCache(val session: FirSession) {
    private val callablesCache: FirCache<FirClassSymbol<*>, Map<Name, List<Category>>?, Nothing?> =
        session.firCachesFactory.createCache { k, _ -> k.computeCallables() }

    operator fun get(symbol: FirClassSymbol<*>): Map<Name, List<Category>>? = callablesCache.getValue(symbol)

    enum class BuilderKind {
        Direct,
        SetOpt,
    }

    fun FirClassSymbol<*>.computeBuilderKind(): BuilderKind {
        val scope = unsubstitutedScope(session, ScopeSession(), true, null)

        var allSetOpt = true
        scope.processAllCallables processor@{ callable ->
            if (!allSetOpt) return@processor
            if (callable.resolvedAndJavaizedReturnTypeSymbol(session)?.classId == this.classId) {
                val name = callable.name.asString()
                if (!name.startsWith("set") && !name.startsWith("opt")) {
                    allSetOpt = false
                }
            }
        }

        return if (allSetOpt) BuilderKind.SetOpt else BuilderKind.Direct
    }

    fun FirClassSymbol<*>.computeCallables(): Map<Name, List<Category>> =
        when (computeBuilderKind()) {
            BuilderKind.Direct -> computeCallablesDirect()
            BuilderKind.SetOpt -> computeCallablesSetOpt()
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun FirClassSymbol<*>.computeCallablesDirect(): Map<Name, List<Category>> {
        val scope = unsubstitutedScope(session, ScopeSession(), true, null)
        val storage = mutableMapOf<Name, MutableList<Category>>()
        scope.getDeclaredConstructors().singleOrNull()?.let { constructor ->
            for (parameter in constructor.valueParameterSymbols) {
                storage.getOrPutIfMissing(parameter.name, { mutableListOf() }).add(Category.Constructor(parameter))
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
        return storage
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun FirClassSymbol<*>.computeCallablesSetOpt(): Map<Name, List<Category>> {
        val scope = unsubstitutedScope(session, ScopeSession(), true, null)
        val storage = mutableMapOf<Name, MutableList<Category>>()
        scope.processAllCallables processor@{ callable ->
            val name = callable.name
            val nameS = name.asString()
            when {
                callable.resolvedAndJavaizedReturnTypeSymbol(session)?.classId != this.classId -> {}
                callable !is FirFunctionSymbol<*> -> {}
                callable.valueParameterSymbols.any { it.isVararg } -> {}
                nameS.startsWith("set") -> {
                    val newName = Name.identifier(nameS.drop(3).decapitalizeAsciiOnly())
                    storage.getOrPutIfMissing(newName, { mutableListOf() }).add(Category.Required(callable))
                }
                callable.valueParameterSymbols.size != 1 -> {
                    val newName = Name.identifier(nameS.drop(3).decapitalizeAsciiOnly())
                    storage.getOrPutIfMissing(newName, { mutableListOf() }).add(Category.Function(callable))
                }
                else -> {
                    val newName = Name.identifier(nameS.drop(3).decapitalizeAsciiOnly())
                    storage.getOrPutIfMissing(newName, { mutableListOf() }).add(Category.Property(callable))
                }
            }
        }
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

    fun getRequiredNames(symbol: FirClassSymbol<*>): Set<Name> =
        get(symbol).orEmpty().filterValues { categories ->
            categories.any { it is Category.Constructor || it is Category.Required }
        }.keys
}

sealed interface Category {
    val symbol: FirCallableSymbol<*>

    sealed interface CreatesProperty : Category
    data class Property(override val symbol: FirFunctionSymbol<*>) : CreatesProperty
    data class Required(override val symbol: FirFunctionSymbol<*>) : CreatesProperty
    data class Constructor(override val symbol: FirValueParameterSymbol) : CreatesProperty

    sealed interface CreatesFunction : Category {
        override val symbol: FirFunctionSymbol<*>
    }
    data class Singular(override val symbol: FirFunctionSymbol<*>) : CreatesFunction
    data class Function(override val symbol: FirFunctionSymbol<*>) : CreatesFunction
}