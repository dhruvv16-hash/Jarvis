package com.jarvispoc.validation

import com.jarvispoc.validation.device.DeviceEnvironment

interface RegressionDetector {
    fun hasCapabilityRegressed(capability: CapabilityCertification, recentEvidence: List<ExecutionEvidence>): Boolean
    fun hasDriverRegressed(appId: String, capabilityId: String, driver: String, recentEvidence: List<ExecutionEvidence>): Boolean
}

class DefaultRegressionDetector(private val threshold: Float = 0.7f) : RegressionDetector {
    override fun hasCapabilityRegressed(capability: CapabilityCertification, recentEvidence: List<ExecutionEvidence>): Boolean {
        if (recentEvidence.isEmpty()) return false
        val recentSuccesses = recentEvidence.count { it.result == "PASS" }
        val recentRate = recentSuccesses.toFloat() / recentEvidence.size
        return recentRate < threshold && capability.status == ValidationStatus.CERTIFIED
    }

    override fun hasDriverRegressed(appId: String, capabilityId: String, driver: String, recentEvidence: List<ExecutionEvidence>): Boolean {
        val driverEvidence = recentEvidence.filter { it.driverType == driver }
        if (driverEvidence.isEmpty()) return false
        val recentSuccesses = driverEvidence.count { it.result == "PASS" }
        return (recentSuccesses.toFloat() / driverEvidence.size) < threshold
    }
}
