package com.jarvispoc.apps.onboarding

import java.util.UUID

interface AppInspectionEngine {
    suspend fun inspectApp(appId: String): List<UiElement>
    suspend fun classifyScreen(elements: List<UiElement>): ScreenRole
}

interface CapabilityDiscoveryEngine {
    suspend fun discoverCapabilities(
        appId: String, 
        currentObservations: List<UiElement>, 
        requestedGoal: String?
    ): List<CapabilityHypothesis>
}

class DefaultCapabilityDiscoveryEngine : CapabilityDiscoveryEngine {
    override suspend fun discoverCapabilities(
        appId: String,
        currentObservations: List<UiElement>,
        requestedGoal: String?
    ): List<CapabilityHypothesis> {
        val hypotheses = mutableListOf<CapabilityHypothesis>()
        
        // Example: Discover search capability if a search input exists
        val hasSearchInput = currentObservations.any { it.editable && (it.text?.contains("search", ignoreCase = true) == true || it.contentDescription?.contains("search", ignoreCase = true) == true) }
        if (hasSearchInput && (requestedGoal == null || requestedGoal.contains("search", ignoreCase = true))) {
            hypotheses.add(CapabilityHypothesis(
                id = UUID.randomUUID().toString(),
                capabilityId = "grocery.search",
                appId = appId,
                evidence = listOf("Found editable search input on screen"),
                confidence = 0.8f,
                status = HypothesisStatus.HYPOTHESIZED,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }

        // Example: Discover add to cart capability
        val hasAddToCart = currentObservations.any { it.clickable && (it.text?.contains("add", ignoreCase = true) == true) }
        if (hasAddToCart && (requestedGoal == null || requestedGoal.contains("add", ignoreCase = true) || requestedGoal.contains("cart", ignoreCase = true))) {
             hypotheses.add(CapabilityHypothesis(
                id = UUID.randomUUID().toString(),
                capabilityId = "grocery.cart.add",
                appId = appId,
                evidence = listOf("Found clickable Add button"),
                confidence = 0.75f,
                status = HypothesisStatus.HYPOTHESIZED,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }
        
        return hypotheses
    }
}
