package com.jarvispoc.execution

import com.jarvispoc.perception.*
import com.jarvispoc.tools.ToolResultStatus

interface AdaptiveDriver {
    val driverId: String
    val cost: Float
    suspend fun execute(target: TargetCandidate): ActionReceipt
}

class ExecutionStrategy(
    private val resolver: TargetResolver,
    private val drivers: List<AdaptiveDriver>
) {
    suspend fun executeAdaptive(target: UiTarget, elements: List<com.jarvispoc.apps.onboarding.UiElement>): ActionReceipt {
        // 1. Resolve candidates
        val candidates = resolver.resolve(target, elements)
        
        if (candidates.isEmpty()) {
            return ActionReceipt(
                executionId = "err",
                requestedAction = target.semanticRole ?: "unknown",
                driver = "none",
                timestamp = System.currentTimeMillis(),
                observation = "No candidates found",
                outcome = ActionOutcome.VERIFICATION_FAILED
            )
        }

        val bestCandidate = candidates.first()

        // 2. Select Driver based on cost & confidence (simplified fallback logic)
        val selectedDriver = drivers.firstOrNull { it.driverId == bestCandidate.driver } 
            ?: drivers.minByOrNull { it.cost }
            ?: throw IllegalStateException("No driver available")

        // 3. Execute
        val receipt = selectedDriver.execute(bestCandidate)
        
        // 4. Verify outcome (simulated)
        return if (receipt.outcome == ActionOutcome.DISPATCHED) {
            receipt.copy(outcome = ActionOutcome.VERIFIED)
        } else {
            receipt
        }
    }
}
