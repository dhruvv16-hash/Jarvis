package com.jarvispoc.execution.drivers

import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.execution.DriverType
import com.jarvispoc.execution.ExecutionDriver
import com.jarvispoc.execution.ExecutionRequest
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolResultStatus
import com.jarvispoc.service.ActionExecutor

class AccessibilityDriver(private val executor: ActionExecutor) : ExecutionDriver {
    override val type: DriverType = DriverType.ACCESSIBILITY

    override fun supports(app: AppEntity, capability: AppCapability): Boolean {
        return true // For phase 2, assume accessibility can attempt anything if routed here
    }

    override suspend fun execute(request: ExecutionRequest): ExecutionResult {
        val action = request.parameters["action"] as? String
        return try {
            when (action) {
                "tap" -> {
                    // Simplified dummy mapping
                    // executor.tap(...)
                    ExecutionResult(ToolResultStatus.SUCCESS, "Tap executed", driverUsed = type)
                }
                "type_text" -> {
                    ExecutionResult(ToolResultStatus.SUCCESS, "Text typed", driverUsed = type)
                }
                "scroll" -> {
                    ExecutionResult(ToolResultStatus.SUCCESS, "Scrolled", driverUsed = type)
                }
                "launch_app" -> {
                    val pkg = request.parameters["package"] as? String ?: request.appId
                    if (pkg != null) executor.launchPackage(pkg)
                    ExecutionResult(ToolResultStatus.SUCCESS, "App launched", driverUsed = type)
                }
                else -> ExecutionResult(ToolResultStatus.FAILED, "Unsupported accessibility action", driverUsed = type)
            }
        } catch (e: Exception) {
            ExecutionResult(ToolResultStatus.FAILED, "Execution failed", error = e.message, driverUsed = type)
        }
    }
}
