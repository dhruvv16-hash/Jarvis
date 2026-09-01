package com.jarvispoc.validation.device

import com.jarvispoc.validation.ValidationMode
import com.jarvispoc.validation.TestScenario

data class DeviceEnvironment(
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val screenDimensions: String,
    val orientation: String,
    val locale: String,
    val timezone: String,
    val accessibilityEnabled: Boolean,
    val notificationListenerEnabled: Boolean
)

interface DeviceTestSession {
    val mode: ValidationMode
    val environment: DeviceEnvironment
    suspend fun runScenario(scenario: TestScenario): String
}

class DefaultDeviceTestSession(
    override val mode: ValidationMode,
    override val environment: DeviceEnvironment
) : DeviceTestSession {
    
    override suspend fun runScenario(scenario: TestScenario): String {
        // 1. Check preconditions
        if (!checkPreconditions(scenario)) {
            return "BLOCKED"
        }
        
        // 2. Enforce Safe Test Mode (block dangerous actions)
        if (mode == ValidationMode.SAFE_TEST && isDangerous(scenario.riskLevel)) {
            return "POLICY_BLOCKED"
        }
        
        if (mode == ValidationMode.DRY_RUN) {
            return "SIMULATED_PASS"
        }
        
        // 3. Execution (Simulated for JVM)
        return "PASS"
    }
    
    private fun checkPreconditions(scenario: TestScenario): Boolean {
        // Simulated precondition check
        return true
    }
    
    private fun isDangerous(riskLevel: String): Boolean {
        return riskLevel == "HIGH" || riskLevel == "CRITICAL"
    }
}
