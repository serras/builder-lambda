package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class BuilderLambdaMembersGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    data object Key : GeneratedDeclarationKey()

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val data = classSymbol.generatedCallData ?: return emptySet()
        val callables = callablesCache.getValue(data.builderClass) ?: return emptySet()
        return callables.keys
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val data = owner.generatedCallData ?: return emptyList()
        val callables = callablesCache.getValue(data.builderClass) ?: return emptyList()
        val name = callableId.callableName
        return callables[name].orEmpty().filterIsInstance<Category.CreatesProperty>().map { category ->
            when (category) {
                is Category.Constructor -> createMemberProperty(
                    owner, Key, name, category.symbol.resolvedReturnType,
                    isVal = false, hasBackingField = false
                ) {
                    withGeneratedDefaultInitializer()
                }.symbol

                is Category.Property -> createMemberProperty(
                    owner, Key, name, category.symbol.valueParameterSymbols.first().resolvedReturnType,
                    isVal = false, hasBackingField = false
                ) {
                    withGeneratedDefaultInitializer()
                }.symbol
            }
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val data = owner.generatedCallData ?: return emptyList()
        val callables = callablesCache.getValue(data.builderClass) ?: return emptyList()
        val name = callableId.callableName
        return callables[name].orEmpty().filterIsInstance<Category.CreatesFunction>().map { category ->
            createMemberFunction(
                owner, Key, name, session.builtinTypes.unitType.coneType
            ) {
                for (p in category.symbol.valueParameterSymbols) {
                    valueParameter(p.name, p.resolvedReturnType, isVararg = p.isVararg)
                }
            }.symbol
        }
    }

    private val callablesCache: FirCache<FirClassSymbol<*>, Map<Name, List<Category>>?, Nothing?> =
        session.firCachesFactory.createCache { k, _ -> k.computeCallables(includeConstructors = true) }

    @OptIn(ExperimentalStdlibApi::class)
    fun FirClassSymbol<*>.computeCallables(
        includeConstructors: Boolean,
        storage: MutableMap<Name, MutableList<Category>> = mutableMapOf(),
    ): Map<Name, List<Category>> {
        if (includeConstructors) {
            constructors(session).singleOrNull()?.let { constructor ->
                for (parameter in constructor.valueParameterSymbols) {
                    storage.getOrPutIfMissing(parameter.name, { mutableListOf() }).add(Category.Constructor(parameter))
                }
            }
        }
        processAllDeclaredCallables(session) processor@{ callable ->
            when {
                callable.resolvedReturnType.classId != this.classId -> {}
                callable !is FirFunctionSymbol<*> -> {}
                callable.valueParameterSymbols.any { it.isVararg } -> { }
                callable.valueParameterSymbols.size != 1 -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Function(callable))
                }
                hasPluralFor(callable.name) -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Singular(callable))
                }
                else -> {
                    storage.getOrPutIfMissing(callable.name, { mutableListOf() }).add(Category.Property(callable))
                }
            }
        }
        getSuperTypes(session).forEach { superType ->
            superType.toClassSymbol(session)?.computeCallables(includeConstructors = false, storage)
        }
        return storage
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

    fun FirClassSymbol<*>.hasPluralFor(name: Name): Boolean {
        if (name.isSpecial) return false
        val singularName = name.asString()
        var found = false
        processAllDeclaredCallables(session) processor@{
            if (found || it.name.isSpecial) return@processor
            val thisName = it.name.asString()
            if (Singulars.from(thisName) == singularName) {
                found = true
            }
        }
        return found
    }
}