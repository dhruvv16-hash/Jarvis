package com.jarvispoc.agent.learning

import com.jarvispoc.agent.Goal

interface ProcedureRepository {
    suspend fun findByGoalAndApp(goal: String, appId: String): List<Procedure>
    suspend fun findByCapability(capabilityId: String, appId: String): List<Procedure>
    suspend fun save(procedure: Procedure)
    suspend fun markNeedsValidation(appId: String, newAppVersion: String)
}

interface ProcedureMatcher {
    suspend fun matchProcedures(goal: Goal, appId: String, capabilityId: String?): List<Procedure>
}

class DefaultProcedureMatcher(
    private val procedureRepo: ProcedureRepository
) : ProcedureMatcher {
    override suspend fun matchProcedures(goal: Goal, appId: String, capabilityId: String?): List<Procedure> {
        val byGoal = procedureRepo.findByGoalAndApp(goal.objective, appId)
        val byCap = capabilityId?.let { procedureRepo.findByCapability(it, appId) } ?: emptyList()
        
        return (byGoal + byCap)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Procedure> { 
                when (it.type) {
                    ProcedureType.MANUAL -> 100f
                    else -> it.confidence
                } 
            }.thenByDescending { it.successCount })
    }
}

class ProcedureValidator {
    fun isValid(procedure: Procedure): Boolean {
        if (procedure.steps.isEmpty()) return false
        if (procedure.confidence < 0.1f) return false
        return true
    }
}
