package com.jarvispoc.execution

import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.manager.AppRegistry
import com.jarvispoc.data.repository.ExecutionHistoryRepository
import com.jarvispoc.security.PolicyEngine
import com.jarvispoc.security.ConfirmationManager
import com.jarvispoc.tools.ToolCall
import com.jarvispoc.tools.ToolResultStatus
import com.jarvispoc.data.entity.ExecutionRecordEntity
import java.util.UUID

class CapabilityRouter(
    private val appRegistry: AppRegistry,
    private val driverRegistry: DriverRegistry,
    private val policyEngine: PolicyEngine,
    private val confirmationManager: ConfirmationManager,
    private val historyRepository: ExecutionHistoryRepository
) {
    suspend fun route(toolCall: ToolCall, capability: AppCapability, appId: String): ExecutionResult {
        val risk = policyEngine.evaluate(toolCall)
        if (confirmationManager.requestConfirmation(toolCall, risk).not()) {
            return ExecutionResult(ToolResultStatus.WAITING_FOR_USER, "User denied or confirmation required.", error = "DENIED", driverUsed = DriverType.NATIVE_API)
        }

        val app = appRegistry.getApp(appId)
        if (app == null || !app.installed) {
            return ExecutionResult(ToolResultStatus.UNAVAILABLE, "App $appId is not installed.", error = "APP_NOT_INSTALLED", driverUsed = DriverType.NATIVE_API)
        }

        val candidates = driverRegistry.getAllDrivers().filter { it.supports(app, capability) }
        if (candidates.isEmpty()) {
            return ExecutionResult(ToolResultStatus.UNAVAILABLE, "No driver supports capability ${capability.name} for app $appId.", error = "DRIVER_NOT_AVAILABLE", driverUsed = DriverType.NATIVE_API)
        }

        val bestDriver = candidates.first() // simplified scoring

        val request = ExecutionRequest(
            requestId = UUID.randomUUID().toString(),
            taskId = toolCall.taskId,
            sessionId = toolCall.sessionId,
            appId = appId,
            capabilityId = capability.name,
            driverType = bestDriver.type,
            parameters = toolCall.arguments,
            riskLevel = risk
        )

        val startTime = System.currentTimeMillis()
        val result = bestDriver.execute(request)
        val endTime = System.currentTimeMillis()

        historyRepository.insertExecution(
            ExecutionRecordEntity(
                id = request.requestId,
                taskId = request.taskId,
                sessionId = request.sessionId ?: "",
                goal = "Capability Execution",
                appId = request.appId,
                capabilityId = request.capabilityId,
                driverType = request.driverType?.name,
                status = result.status.name,
                startedAt = startTime,
                completedAt = endTime,
                duration = endTime - startTime,
                failureReason = result.error,
                userConfirmed = true,
                createdAt = System.currentTimeMillis()
            )
        )

        return result
    }
}
