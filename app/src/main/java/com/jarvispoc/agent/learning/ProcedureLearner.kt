package com.jarvispoc.agent.learning

import com.jarvispoc.execution.ExecutionResult
import java.util.UUID

interface ProcedureLearner {
    suspend fun observeSuccess(
        goal: String, 
        appId: String, 
        capabilityId: String, 
        executedSteps: List<ProcedureStep>, 
        appVersion: String?
    )
    suspend fun observeFailure(
        procedureId: String, 
        appId: String, 
        appVersion: String?, 
        failureReason: String, 
        observation: String
    )
}

class DefaultProcedureLearner(
    private val procedureRepo: ProcedureRepository,
    private val failureRepo: FailureRepository
) : ProcedureLearner {
    override suspend fun observeSuccess(
        goal: String, 
        appId: String, 
        capabilityId: String, 
        executedSteps: List<ProcedureStep>, 
        appVersion: String?
    ) {
        val procedure = Procedure(
            id = UUID.randomUUID().toString(),
            name = "Learned Procedure for $goal",
            goalDescription = goal,
            appId = appId,
            capabilityId = capabilityId,
            type = ProcedureType.LEARNED,
            steps = executedSteps,
            appVersion = appVersion,
            confidence = 0.5f, // Initial confidence
            successCount = 1,
            failureCount = 0,
            lastSuccessAt = System.currentTimeMillis(),
            lastFailureAt = null
        )
        procedureRepo.save(procedure)
    }

    override suspend fun observeFailure(
        procedureId: String, 
        appId: String, 
        appVersion: String?, 
        failureReason: String, 
        observation: String
    ) {
        failureRepo.save(
            FailurePattern(
                appId = appId,
                procedureId = procedureId,
                appVersion = appVersion,
                failureType = failureReason,
                observation = observation,
                recovery = null,
                confidence = 0.8f,
                occurrenceCount = 1,
                lastSeen = System.currentTimeMillis()
            )
        )
    }
}

interface FailureRepository {
    suspend fun save(pattern: FailurePattern)
}

interface AdaptationEngine {
    suspend fun adaptProcedure(
        failedProcedure: Procedure, 
        currentObservation: String, 
        failureReason: String
    ): Procedure?
}

class DefaultAdaptationEngine : AdaptationEngine {
    override suspend fun adaptProcedure(
        failedProcedure: Procedure, 
        currentObservation: String, 
        failureReason: String
    ): Procedure? {
        // Conceptually, this would use SemanticMatcher and LLM to propose a new step sequence.
        // For Phase 5, we return a candidate ADAPTED procedure if a known pattern matches.
        if (currentObservation.contains("New UI element")) {
            return failedProcedure.copy(
                id = UUID.randomUUID().toString(),
                type = ProcedureType.ADAPTED,
                confidence = 0.3f,
                successCount = 0,
                failureCount = 0
            )
        }
        return null
    }
}
