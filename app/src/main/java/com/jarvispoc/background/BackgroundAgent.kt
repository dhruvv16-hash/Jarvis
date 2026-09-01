package com.jarvispoc.background

import com.jarvispoc.automation.Automation
import com.jarvispoc.automation.AutomationManager
import com.jarvispoc.events.JarvisEvent

data class AgentRequest(
    val source: String,
    val goal: String,
    val metadata: Map<String, String>
)

interface BackgroundAgent {
    suspend fun processRequest(request: AgentRequest): String
}

class FakeBackgroundAgent : BackgroundAgent {
    override suspend fun processRequest(request: AgentRequest): String {
        return if (request.goal.contains("HIGH_RISK")) "WAITING_FOR_USER_CONFIRMATION" else "COMPLETED"
    }
}

interface TaskDispatcher {
    suspend fun dispatch(trigger: Trigger, automation: Automation, event: JarvisEvent): String
}

class DefaultTaskDispatcher(
    private val agent: BackgroundAgent
) : TaskDispatcher {
    private var deviceInteractionLockOwner: String? = null

    override suspend fun dispatch(trigger: Trigger, automation: Automation, event: JarvisEvent): String {
        if (automation.status != com.jarvispoc.automation.AutomationStatus.ACTIVE) {
            return "SKIPPED_PAUSED"
        }
        
        // Concurrency Lock Check
        if (deviceInteractionLockOwner != null && deviceInteractionLockOwner != automation.id) {
            return "WAITING_FOR_DEVICE"
        }
        
        deviceInteractionLockOwner = automation.id
        
        val request = AgentRequest(
            source = "BACKGROUND_SCHEDULE",
            goal = automation.taskTemplate,
            metadata = event.metadata
        )
        
        val result = agent.processRequest(request)
        deviceInteractionLockOwner = null
        
        return result
    }
}
