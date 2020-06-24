/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.FirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPropertyDelegate

class KtPropertyDelegationMethodsReferenceFirImpl(
    element: KtPropertyDelegate
) : KtPropertyDelegationMethodsReference(element), FirKtReference {
    override fun resolveToSymbols(analysisSession: FrontendAnalysisSession): Collection<KtSymbol> {
        check(analysisSession is FirAnalysisSession)
        val property = (expression.parent as? KtElement)?.getOrBuildFirSafe<FirProperty>() ?: return emptyList()
        if (property.delegate == null) return emptyList()
        val getValueSymbol = (property.getter?.singleStatementOfType<FirReturnExpression>()?.result as? FirFunctionCall)?.getCalleeSymbol()
        val setValueSymbol = property.setter?.singleStatementOfType<FirFunctionCall>()?.getCalleeSymbol()
        return listOfNotNull(
            getValueSymbol?.fir?.buildSymbol(analysisSession.symbolBuilder),
            setValueSymbol?.fir?.buildSymbol(analysisSession.symbolBuilder)
        )
    }

    private inline fun <reified S : FirStatement> FirPropertyAccessor.singleStatementOfType(): S? =
        body?.statements?.singleOrNull() as? S
}