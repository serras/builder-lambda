package com.serranofp.builder.lambda.fir

import com.serranofp.builder.lambda.BuilderLambdaIds
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance

@OptIn(FirExtensionApiInternals::class)
class CallRefinement(session: FirSession) : FirFunctionCallRefinementExtension(session) {
    data object Key : GeneratedDeclarationKey()

    override fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType? {
        // see if we have a builder
        if (symbol.callableId != BuilderLambdaIds.BUILD_FUNCTION_ID) return null

        // if the type is not resolved, we find it in a checker
        val typeToBuild =  callInfo.typeArguments.firstOrNull()?.toConeTypeProjection()?.type ?: return callInfo.updateTypeArgumentToNothing()
        val classToBuild = typeToBuild.toClassSymbol(session) ?: return callInfo.updateTypeArgumentToNothing()
        val builderFunction = classToBuild.builderFunction(session) ?: return callInfo.updateTypeArgumentToNothing()
        val builderClass = builderFunction.resolvedReturnType.toClassSymbol(session) ?: return callInfo.updateTypeArgumentToNothing()

        val refinedTypeId = localClassId(Name.identifier("Local${classToBuild.name.asStringStripSpecialMarkers()}Builder"))
        val refinedTypeSymbol = FirRegularClassSymbol(refinedTypeId)
        val refinedTypeDeclaration = buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(Key)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()

            name = refinedTypeId.shortClassName
            this.symbol = refinedTypeSymbol
        }

        val callData = GeneratedCallData(classToBuild, builderClass, refinedTypeDeclaration)
        refinedTypeDeclaration.generatedCallData = callData
        // transform * => the local class
        callInfo.updateTypeArgument(
            buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    refinedTypeDeclaration.symbol.toLookupTag(),
                    arrayOf(),
                    isMarkedNullable = false
                )
            }
        )

        return CallReturnType(typeToBuild.toFirResolvedTypeRef()) { functionSymbol ->
            session.callDataStorage.generatedCallData.getValue(functionSymbol, callData)
        }
    }

    fun CallInfo.updateTypeArgumentToNothing(): Nothing? {
        updateTypeArgument(session.builtinTypes.nothingType)
        return null
    }

    fun CallInfo.updateTypeArgument(typeRef: FirTypeRef) {
        (callSite as? FirFunctionCall)?.transformTypeArguments(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirStarProjection) {
                    @Suppress("UNCHECKED_CAST")
                    buildTypeProjectionWithVariance {
                        this.typeRef = typeRef
                        this.variance = Variance.INVARIANT
                    } as E
                } else {
                    element
                }
            }
        }, null)
    }

    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall {
        val transformedSymbol = call.calleeReference.resolved?.toResolvedNamedFunctionSymbol() ?: return call
        val originalSource = call.calleeReference.source

        val callData = session.callDataStorage.generatedCallData.getValue(transformedSymbol)
        val localBuilderClass = callData.localBuilderClass
        localBuilderClass.anchor = call.source

        call.transformCalleeReference(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirResolvedNamedReference) {
                    @Suppress("UNCHECKED_CAST")
                    buildResolvedNamedReference {
                        this.name = element.name
                        resolvedSymbol = originalSymbol
                    } as E
                } else {
                    element
                }
            }
        }, null)

        val returnType = call.resolvedType
        val argument = buildAnonymousFunctionExpression {
            val fSymbol = FirAnonymousFunctionSymbol()
            val target = FirFunctionTarget(null, isLambda = true)
            isTrailingLambda = true
            anonymousFunction = buildAnonymousFunction {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(Key)
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                deprecationsProvider = EmptyDeprecationsProvider
                returnTypeRef = buildResolvedTypeRef {
                    coneType = returnType
                }
                body = buildBlock {
                    this.coneTypeOrNull = returnType
                    statements += callData.localBuilderClass

                    statements += buildReturnExpression {
                        result = call
                        this.target = target
                    }
                }
                this.symbol = fSymbol
                isLambda = true
                hasExplicitParameterList = false
                typeRef = buildResolvedTypeRef {
                    coneType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function0"))),
                        typeArguments = arrayOf(returnType),
                        isMarkedNullable = false
                    )
                }
                invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                inlineStatus = InlineStatus.Inline
            }.also { target.bind(it) }
        }

        val runFunction = findRun(session)
        val runParameter = runFunction.valueParameterSymbols[0]
        val newCall = buildFunctionCall {
            this.coneTypeOrNull = returnType
            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef { coneType = returnType }
                variance = Variance.INVARIANT
            }
            argumentList = buildResolvedArgumentList(original = null, linkedMapOf(argument to runParameter.fir))
            calleeReference = buildResolvedNamedReference {
                source = originalSource
                name = Name.identifier("run")
                resolvedSymbol = runFunction
            }
        }
        return newCall
    }

    override fun ownsSymbol(symbol: FirRegularClassSymbol): Boolean {
        return symbol.anchor != null
    }

    override fun anchorElement(symbol: FirRegularClassSymbol): KtSourceElement {
        return symbol.anchor!!
    }

    override fun restoreSymbol(call: FirFunctionCall, name: Name): FirRegularClassSymbol? {
        val newType = (call.resolvedType.typeArguments.firstOrNull() as? ConeClassLikeType)?.toRegularClassSymbol(session)
        return newType?.generatedCallData?.localBuilderClass?.takeIf { it.name == name }?.symbol
    }
}
