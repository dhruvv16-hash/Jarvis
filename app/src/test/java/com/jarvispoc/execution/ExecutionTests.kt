package com.jarvispoc.execution

import com.jarvispoc.apps.onboarding.UiElement
import com.jarvispoc.perception.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExecutionTests {

    private val fakeElementsV1 = listOf(
        UiElement("btn_search", "search", "Search", "Search Input", null, null, true, true, true, false, 1.0f),
        UiElement("btn_add", "add", "Add", null, null, null, true, true, false, false, 1.0f)
    )

    private val fakeElementsV2 = listOf(
        UiElement("btn_find", "search", "Find products", "Search items", null, null, true, true, true, false, 1.0f),
        UiElement("btn_basket", "add", "Add to basket", null, null, null, true, true, false, false, 1.0f)
    )

    private val accessibilityDriver = object : AdaptiveDriver {
        override val driverId = "AccessibilityDriver"
        override val cost = 0.5f
        override suspend fun execute(target: TargetCandidate): ActionReceipt {
            if (target.confidence < 0.5f) {
                return ActionReceipt("exec_1", "action", driverId, System.currentTimeMillis(), "Low confidence", ActionOutcome.VERIFICATION_FAILED)
            }
            return ActionReceipt("exec_1", "action", driverId, System.currentTimeMillis(), "Dispatched successfully", ActionOutcome.DISPATCHED)
        }
    }

    private val deepLinkDriver = object : AdaptiveDriver {
        override val driverId = "DeepLinkDriver"
        override val cost = 0.1f
        override suspend fun execute(target: TargetCandidate): ActionReceipt {
            return ActionReceipt("exec_2", "deeplink", driverId, System.currentTimeMillis(), "Deep link failed", ActionOutcome.VERIFICATION_FAILED)
        }
    }

    @Test
    fun testExactTargetResolution() = runBlocking {
        val resolver = DefaultTargetResolver(DefaultTargetMatchScorer())
        val target = UiTarget(null, "Search", null, "btn_search", null, null, null, null, null, null, "app", 1.0f)
        
        val candidates = resolver.resolve(target, fakeElementsV1)
        assertTrue(candidates.isNotEmpty())
        assertEquals("Exact resource ID match", candidates[0].matchReasons[0])
    }

    @Test
    fun testSemanticTargetResolutionFallback() = runBlocking {
        val resolver = DefaultTargetResolver(DefaultTargetMatchScorer())
        // V1 target looking for "Search" string but we pass V2 elements where it's "Find products"
        val target = UiTarget("search", "Search", null, "btn_search_old", null, null, null, null, null, null, "app", 1.0f)
        
        val candidates = resolver.resolve(target, fakeElementsV2)
        assertTrue(candidates.isNotEmpty())
        // Should match based on semanticRole "search"
        assertTrue(candidates[0].matchReasons.contains("Semantic role match"))
        assertEquals("btn_find", candidates[0].resolvedNode?.elementId)
    }

    @Test
    fun testAdaptiveExecutionDriverSelection() = runBlocking {
        val resolver = DefaultTargetResolver(DefaultTargetMatchScorer())
        val strategy = ExecutionStrategy(resolver, listOf(deepLinkDriver, accessibilityDriver))
        
        val target = UiTarget("add", null, null, null, null, null, null, null, null, null, "app", 1.0f)
        val receipt = strategy.executeAdaptive(target, fakeElementsV2)
        
        // Adaptive driver executed and verified
        assertEquals(ActionOutcome.VERIFIED, receipt.outcome)
        assertEquals("AccessibilityDriver", receipt.driver)
    }
}
