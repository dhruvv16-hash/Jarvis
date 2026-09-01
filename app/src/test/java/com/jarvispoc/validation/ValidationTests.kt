package com.jarvispoc.validation

import com.jarvispoc.validation.device.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ValidationTests {

    private val testEnv = DeviceEnvironment(
        apiLevel = 35,
        manufacturer = "Google",
        model = "Pixel 8",
        osVersion = "14",
        screenDimensions = "1080x2400",
        orientation = "PORTRAIT",
        locale = "en-US",
        timezone = "UTC",
        accessibilityEnabled = true,
        notificationListenerEnabled = true
    )

    @Test
    fun testSafeTestModeBlocksDangerousAction() = runBlocking {
        val session = DefaultDeviceTestSession(ValidationMode.SAFE_TEST, testEnv)
        
        val scenario = TestScenario(
            id = "scenario_purchase",
            name = "Amazon Purchase",
            appId = "com.amazon.mShop.android.shopping",
            capabilityId = "shopping.purchase",
            preconditions = emptyList(),
            actions = emptyList(),
            expectedOutcome = "Order Confirmation",
            riskLevel = "HIGH"
        )
        
        val result = session.runScenario(scenario)
        assertEquals("POLICY_BLOCKED", result)
    }

    @Test
    fun testSafeTestModeAllowsSafeAction() = runBlocking {
        val session = DefaultDeviceTestSession(ValidationMode.SAFE_TEST, testEnv)
        
        val scenario = TestScenario(
            id = "scenario_search",
            name = "Amazon Search",
            appId = "com.amazon.mShop.android.shopping",
            capabilityId = "shopping.search",
            preconditions = emptyList(),
            actions = emptyList(),
            expectedOutcome = "Results Visible",
            riskLevel = "LOW"
        )
        
        val result = session.runScenario(scenario)
        assertEquals("PASS", result)
    }

    @Test
    fun testRegressionDetection() {
        val detector = DefaultRegressionDetector(threshold = 0.8f) // Expect 80% success rate
        
        val cert = CapabilityCertification(
            appId = "com.example",
            capabilityId = "test.cap",
            driverType = "Accessibility",
            androidVersion = 35,
            appVersion = "1.0",
            deviceModel = "Pixel",
            status = ValidationStatus.CERTIFIED,
            successCount = 100,
            failureCount = 2,
            lastVerifiedAt = System.currentTimeMillis(),
            confidence = 0.98f
        )
        
        val evidence = listOf(
            ExecutionEvidence("1", "com.example", "test.cap", "Accessibility", 35, "1.1", "PASS", 0L),
            ExecutionEvidence("2", "com.example", "test.cap", "Accessibility", 35, "1.1", "FAIL", 0L, FailureType.TARGET_NOT_FOUND),
            ExecutionEvidence("3", "com.example", "test.cap", "Accessibility", 35, "1.1", "FAIL", 0L, FailureType.UI_CHANGED)
        ) // 1 pass out of 3 = 33%
        
        val regressed = detector.hasCapabilityRegressed(cert, evidence)
        assertTrue("Capability should be marked as regressed due to low recent success rate", regressed)
    }
}
