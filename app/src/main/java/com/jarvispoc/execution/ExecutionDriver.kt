package com.jarvispoc.execution

import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.security.RiskClassification
import com.jarvispoc.tools.ToolResultStatus
import com.jarvispoc.perception.Observation

enum class DriverType { NATIVE_API, OFFICIAL_API, INTENT, DEEPLINK, ACCESSIBILITY, VISION }

data class ExecutionRequest(
    val requestId: String,
    val taskId: String?,
    val sessionId: String?,
    val appId: String?,
    val capabilityId: String,
    val driverType: DriverType?,
    val parameters: Map<String, Any>,
    val riskLevel: RiskClassification
)

data class ExecutionResult(
    val status: ToolResultStatus,
    val summary: String,
    val structuredData: Map<String, Any>? = null,
    val observations: List<Observation>? = null,
    val error: String? = null,
    val driverUsed: DriverType,
    val retryable: Boolean = false
)

interface ExecutionDriver {
    val type: DriverType
    fun supports(app: AppEntity, capability: AppCapability): Boolean
    suspend fun execute(request: ExecutionRequest): ExecutionResult
}
