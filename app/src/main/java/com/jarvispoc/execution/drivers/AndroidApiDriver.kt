package com.jarvispoc.execution.drivers

import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.execution.DriverType
import com.jarvispoc.execution.ExecutionDriver
import com.jarvispoc.execution.ExecutionRequest
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.tools.ToolResultStatus

class AndroidApiDriver : ExecutionDriver {
    override val type: DriverType = DriverType.NATIVE_API

    override fun supports(app: AppEntity, capability: AppCapability): Boolean {
        return capability.name.startsWith("device.")
    }

    override suspend fun execute(request: ExecutionRequest): ExecutionResult {
        // Placeholder for Android APIs like AlarmManager, TelecomManager, etc.
        return ExecutionResult(ToolResultStatus.SUCCESS, "Executed Android API for $request.capabilityId", driverUsed = type)
    }
}
