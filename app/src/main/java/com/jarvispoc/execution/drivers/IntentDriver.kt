package com.jarvispoc.execution.drivers

import android.content.Context
import android.content.Intent
import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.execution.DriverType
import com.jarvispoc.execution.ExecutionDriver
import com.jarvispoc.execution.ExecutionRequest
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolResultStatus

class IntentDriver(private val context: Context) : ExecutionDriver {
    override val type: DriverType = DriverType.INTENT

    override fun supports(app: AppEntity, capability: AppCapability): Boolean {
        return true
    }

    override suspend fun execute(request: ExecutionRequest): ExecutionResult {
        val action = request.parameters["intentAction"] as? String
        if (action != null) {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return ExecutionResult(ToolResultStatus.SUCCESS, "Intent $action broadcasted", driverUsed = type)
        }
        return ExecutionResult(ToolResultStatus.FAILED, "Missing intentAction parameter", error = "INVALID_ARGS", driverUsed = type)
    }
}
