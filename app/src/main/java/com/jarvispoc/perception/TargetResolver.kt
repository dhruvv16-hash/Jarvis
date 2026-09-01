package com.jarvispoc.perception

import com.jarvispoc.apps.onboarding.UiElement

interface TargetResolver {
    suspend fun resolve(target: UiTarget, currentElements: List<UiElement>): List<TargetCandidate>
}

class DefaultTargetMatchScorer {
    fun score(target: UiTarget, element: UiElement): Pair<Float, List<String>> {
        var score = 0.0f
        val reasons = mutableListOf<String>()

        if (target.resourceId != null && target.resourceId == element.elementId) {
            score += 0.9f
            reasons.add("Exact resource ID match")
        }

        if (target.text != null && element.text?.contains(target.text, ignoreCase = true) == true) {
            score += 0.8f
            reasons.add("Text match")
        }

        if (target.contentDescription != null && element.contentDescription?.contains(target.contentDescription, ignoreCase = true) == true) {
            score += 0.7f
            reasons.add("Content description match")
        }

        if (target.semanticRole != null && target.semanticRole == element.role) {
            score += 0.85f
            reasons.add("Semantic role match")
        }
        
        // Normalize
        val finalScore = score.coerceAtMost(1.0f)
        return Pair(finalScore, reasons)
    }
}

class DefaultTargetResolver(
    private val scorer: DefaultTargetMatchScorer
) : TargetResolver {
    override suspend fun resolve(target: UiTarget, currentElements: List<UiElement>): List<TargetCandidate> {
        val candidates = mutableListOf<TargetCandidate>()
        
        for (element in currentElements) {
            val (score, reasons) = scorer.score(target, element)
            if (score > 0.3f) {
                candidates.add(
                    TargetCandidate(
                        uiTarget = target,
                        resolvedNode = element,
                        matchReasons = reasons,
                        confidence = score,
                        driver = "AccessibilityDriver"
                    )
                )
            }
        }
        
        return candidates.sortedByDescending { it.confidence }
    }
}
