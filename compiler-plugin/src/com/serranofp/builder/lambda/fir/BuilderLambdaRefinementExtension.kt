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
class BuilderLambdaRefinementExtension(session: FirSession) : FirFunctionCallRefinementExtension(session) {
    data object Key : GeneratedDeclarationKey()

    override fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType? {
        // see if we have a builder
        if (symbol.callableId != BuilderLambdaIds.FUNCTION_ID) return null

        // if the type is not resolved, we find it in a checker
        val typeToBuild =  callInfo.typeArguments.first().toConeTypeProjection().type ?: return null
        val classToBuild = typeToBuild.toClassSymbol(session) ?: return null
        val builderFunction = classToBuild.builderFunction(session) ?: return null
        val builderClass = builderFunction.resolvedReturnType.toClassSymbol(session) ?: return null

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
            superTypeRefs += buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(BuilderLambdaIds.CLASS_ID),
                    typeArguments = arrayOf(typeToBuild),
                    isMarkedNullable = false
                )
            }
        }

        val typeRef = buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(
                refinedTypeSymbol.toLookupTag(),
                arrayOf(),
                isMarkedNullable = false
            )
        }

        val callData = GeneratedCallData(classToBuild, builderClass, refinedTypeDeclaration)
        refinedTypeDeclaration.generatedCallData = callData

        return CallReturnType(typeRef) { functionSymbol ->
            session.callDataStorage.generatedCallData.getValue(functionSymbol, callData)
        }
    }

    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall {
        val resolvedLet = findLet(session)
        val parameter = resolvedLet.valueParameterSymbols[0]

        val explicitReceiver = call.explicitReceiver ?: return call
        val receiverType = explicitReceiver.resolvedType
        val returnType = call.resolvedType
        val originalSource = call.calleeReference.source
        val callDispatchReceiver = call.dispatchReceiver
        val callExtensionReceiver = call.extensionReceiver

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

        val symbol = call.calleeReference.resolved?.toResolvedNamedFunctionSymbol() ?: return call
        val callData = session.callDataStorage.generatedCallData.getValue(symbol)
        val localBuilderClass = callData.localBuilderClass
        localBuilderClass.anchor = call.source

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
                val itName = Name.identifier("it")
                val parameterSymbol = FirValueParameterSymbol()
                valueParameters += buildValueParameter {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(Key)
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = receiverType
                    }
                    name = itName
                    this.symbol = parameterSymbol
                    containingDeclarationSymbol = fSymbol
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
                body = buildBlock {
                    this.coneTypeOrNull = returnType
                    statements += callData.localBuilderClass

                    statements += buildReturnExpression {
                        val itPropertyAccess = buildPropertyAccessExpression {
                            coneTypeOrNull = receiverType
                            calleeReference = buildResolvedNamedReference {
                                name = parameterSymbol.name
                                resolvedSymbol = parameterSymbol
                            }
                        }
                        if (callDispatchReceiver != null) {
                            call.replaceDispatchReceiver(itPropertyAccess)
                        }
                        call.replaceExplicitReceiver(itPropertyAccess)
                        if (callExtensionReceiver != null) {
                            call.replaceExtensionReceiver(itPropertyAccess)
                        }

                        result = call
                        this.target = target
                    }
                }
                this.symbol = fSymbol
                isLambda = true
                hasExplicitParameterList = false
                typeRef = buildResolvedTypeRef {
                    coneType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function1"))),
                        typeArguments = arrayOf(receiverType, returnType),
                        isMarkedNullable = false
                    )
                }
                invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                inlineStatus = InlineStatus.Inline
            }.also { target.bind(it) }
        }

        val newCall = buildFunctionCall {
            this.coneTypeOrNull = returnType
            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    coneType = receiverType
                }
                variance = Variance.INVARIANT
            }

            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    coneType = returnType
                }
                variance = Variance.INVARIANT
            }
            dispatchReceiver = call.dispatchReceiver
            this.explicitReceiver = call.explicitReceiver
            extensionReceiver = call.extensionReceiver
            argumentList = buildResolvedArgumentList(original = null, linkedMapOf(argument to parameter.fir))
            calleeReference = buildResolvedNamedReference {
                source = originalSource
                name = Name.identifier("let")
                resolvedSymbol = resolvedLet
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
