package com.jarvispoc.automation

enum class AutomationStatus {
    DRAFT, ACTIVE, PAUSED, WAITING, COMPLETED, FAILED, CANCELLED, DISABLED
}

data class Automation(
    val id: String,
    val name: String,
    val description: String,
    val taskTemplate: String,
    val triggerId: String,
    var status: AutomationStatus,
    val riskLevel: String,
    val maxRetries: Int
)

data class AutomationRun(
    val runId: String,
    val automationId: String,
    val taskId: String,
    val startedAt: Long,
    val status: String
)

interface AutomationManager {
    fun createAutomation(automation: Automation)
    fun pauseAutomation(id: String)
    fun resumeAutomation(id: String)
    fun getActiveAutomations(): List<Automation>
}

class DefaultAutomationManager : AutomationManager {
    private val automations = mutableListOf<Automation>()
    
    override fun createAutomation(automation: Automation) {
        automations.add(automation)
    }
    
    override fun pauseAutomation(id: String) {
        automations.find { it.id == id }?.status = AutomationStatus.PAUSED
    }
    
    override fun resumeAutomation(id: String) {
        automations.find { it.id == id }?.status = AutomationStatus.ACTIVE
    }
    
    override fun getActiveAutomations(): List<Automation> {
        return automations.filter { it.status == AutomationStatus.ACTIVE }
    }
}
