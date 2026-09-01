package com.jarvispoc.validation

enum class ValidationStatus {
    UNKNOWN, EXPERIMENTAL, TESTED, RELIABLE, CERTIFIED, STALE, FAILED, BLOCKED
}

enum class ValidationMode {
    NORMAL, SAFE_TEST, DRY_RUN
}

enum class FailureType {
    APP_NOT_INSTALLED, APP_NOT_FOUND, APP_LAUNCH_FAILED, AUTH_REQUIRED,
    PERMISSION_REQUIRED, TARGET_NOT_FOUND, TARGET_AMBIGUOUS, DRIVER_UNAVAILABLE,
    DRIVER_FAILED, UI_CHANGED, VERIFICATION_FAILED, UNKNOWN_OUTCOME, TIMEOUT,
    NETWORK_FAILURE, APP_CRASH, APP_STATE_INVALID, USER_CANCELLED, POLICY_BLOCKED,
    DEVICE_UNAVAILABLE
}

data class CapabilityCertification(
    val appId: String,
    val capabilityId: String,
    val driverType: String,
    val androidVersion: Int,
    val appVersion: String,
    val deviceModel: String,
    val status: ValidationStatus,
    val successCount: Int,
    val failureCount: Int,
    val lastVerifiedAt: Long,
    val confidence: Float
)

data class ExecutionEvidence(
    val id: String,
    val appId: String,
    val capabilityId: String,
    val driverType: String,
    val androidVersion: Int,
    val appVersion: String,
    val result: String,
    val verifiedAt: Long,
    val failureType: FailureType? = null
)

data class TestScenario(
    val id: String,
    val name: String,
    val appId: String,
    val capabilityId: String,
    val preconditions: List<String>,
    val actions: List<String>,
    val expectedOutcome: String,
    val riskLevel: String
)

data class ExecutionTrace(
    val id: String,
    val timestamp: Long,
    val taskId: String,
    val action: String,
    val driver: String,
    val target: String?,
    val observation: String?,
    val verification: String?,
    val result: String
)
