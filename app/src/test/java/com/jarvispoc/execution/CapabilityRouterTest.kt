package com.jarvispoc.execution

import com.jarvispoc.apps.AppCapability
import com.jarvispoc.data.dao.AppDao
import com.jarvispoc.data.dao.ExecutionHistoryDao
import com.jarvispoc.data.entity.*
import com.jarvispoc.data.manager.AppRegistry
import com.jarvispoc.data.repository.AppRepository
import com.jarvispoc.data.repository.ExecutionHistoryRepository
import com.jarvispoc.security.ConfirmationManager
import com.jarvispoc.security.PolicyEngine
import com.jarvispoc.security.RiskClassification
import com.jarvispoc.tools.ToolCall
import com.jarvispoc.tools.ToolResultStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityRouterTest {

    private val fakeApp = AppEntity(
        id = "app_1", packageName = "com.test", displayName = "Test",
        versionName = null, versionCode = null, installed = true, enabled = true,
        firstSeenAt = 0, lastSeenAt = 0, updatedAt = 0
    )
    
    private val dummyAppDao = object : AppDao {
        override fun insertApp(app: AppEntity) {}
        override fun insertCapability(capability: AppCapabilityEntity) {}
        override fun insertDriver(driver: AppDriverEntity) {}
        override fun insertState(state: AppStateEntity) {}
        override fun getApp(id: String): AppEntity? = if (id == "app_1") fakeApp else null
        override fun getAllApps(): List<AppEntity> = emptyList()
        override fun getCapabilities(appId: String): List<AppCapabilityEntity> = emptyList()
        override fun getDrivers(appId: String): List<AppDriverEntity> = emptyList()
        override fun getState(appId: String): AppStateEntity? = null
    }
    
    private val dummyHistoryDao = object : ExecutionHistoryDao {
        override fun insertExecution(record: ExecutionRecordEntity) {}
        override fun getExecution(id: String): ExecutionRecordEntity? = null
        override fun insertActionAttempt(attempt: ActionAttemptEntity) {}
        override fun insertObservation(observation: ObservationEntity) {}
    }

    private val fakeAppRegistry = object : AppRegistry(AppRepository(dummyAppDao)) {
        override suspend fun getApp(id: String): AppEntity? = if (id == "app_1") fakeApp else null
        override suspend fun getDrivers(appId: String) = emptyList<com.jarvispoc.data.entity.AppDriverEntity>()
    }

    private val fakeDriver = object : ExecutionDriver {
        override val type = DriverType.ACCESSIBILITY
        override fun supports(app: AppEntity, capability: AppCapability) = true
        override suspend fun execute(request: ExecutionRequest): ExecutionResult {
            return ExecutionResult(ToolResultStatus.SUCCESS, "Fake execution", driverUsed = type)
        }
    }

    private val fakeDriverRegistry = DriverRegistry().apply { register(fakeDriver) }

    private val fakePolicyEngine = object : PolicyEngine {
        override suspend fun evaluate(action: ToolCall): RiskClassification = RiskClassification.LOW
    }

    private val fakeConfirmationManager = object : ConfirmationManager {
        override suspend fun requestConfirmation(action: ToolCall, risk: RiskClassification): Boolean = true
        override fun requiresConfirmation(action: ToolCall, risk: RiskClassification): Boolean = false
    }

    private val fakeHistoryRepo = object : ExecutionHistoryRepository(dummyHistoryDao) {
        override suspend fun insertExecution(record: ExecutionRecordEntity) { /* No-op */ }
    }

    @Test
    fun testRouterSuccessfullyExecutesCapability() = runBlocking {
        val router = CapabilityRouter(fakeAppRegistry, fakeDriverRegistry, fakePolicyEngine, fakeConfirmationManager, fakeHistoryRepo)
        
        val call = ToolCall(id = "1", toolId = "tool_1", capabilityId = "test.cap", arguments = emptyMap(), taskId = null, sessionId = null)
        val result = router.route(call, AppCapability("test.cap", ""), "app_1")

        assertEquals(ToolResultStatus.SUCCESS, result.status)
        assertEquals(DriverType.ACCESSIBILITY, result.driverUsed)
    }
}
