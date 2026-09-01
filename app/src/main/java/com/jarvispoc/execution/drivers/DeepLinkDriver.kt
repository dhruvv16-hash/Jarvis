package com.jarvispoc.execution.drivers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.execution.DriverType
import com.jarvispoc.execution.ExecutionDriver
import com.jarvispoc.execution.ExecutionRequest
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolResultStatus

class DeepLinkDriver(private val context: Context) : ExecutionDriver {
    override val type: DriverType = DriverType.DEEPLINK

    override fun supports(app: AppEntity, capability: AppCapability): Boolean {
        return capability.name.contains("search") || capability.name.contains("view")
    }

    override suspend fun execute(request: ExecutionRequest): ExecutionResult {
        val uriStr = request.parameters["uri"] as? String
        if (uriStr != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return ExecutionResult(ToolResultStatus.SUCCESS, "DeepLink $uriStr launched", driverUsed = type)
        }
        return ExecutionResult(ToolResultStatus.FAILED, "Missing uri parameter", error = "INVALID_ARGS", driverUsed = type)
    }
}
