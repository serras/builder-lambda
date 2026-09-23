package com.serranofp.builder.lambda.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class MembersGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    data object Key : GeneratedDeclarationKey()

    private val callables = CallablesCache(session)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val data = classSymbol.generatedCallData ?: return emptySet()
        val callables = callables[data.builderClass] ?: return emptySet()
        return callables.keys
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val data = owner.generatedCallData ?: return emptyList()
        val callables = callables[data.builderClass] ?: return emptyList()
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
        val callables = callables[data.builderClass] ?: return emptyList()
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
}