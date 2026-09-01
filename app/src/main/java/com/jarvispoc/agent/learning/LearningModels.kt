package com.jarvispoc.agent.learning

import com.jarvispoc.tools.ToolCall

enum class ProcedureType {
    MANUAL, LEARNED, ADAPTED, IMPORTED
}

data class ProcedureStep(
    val sequence: Int,
    val action: String,
    val target: Map<String, String>,
    val expectedObservation: String?,
    val verification: String?,
    val fallback: String?
)

data class Procedure(
    val id: String,
    val name: String,
    val goalDescription: String,
    val appId: String,
    val capabilityId: String,
    val type: ProcedureType,
    val steps: List<ProcedureStep>,
    val appVersion: String?,
    val confidence: Float,
    val successCount: Int,
    val failureCount: Int,
    val lastSuccessAt: Long?,
    val lastFailureAt: Long?
)

data class UiKnowledge(
    val appId: String,
    val screen: String,
    val semanticRole: String,
    val observedText: String,
    val action: String,
    val confidence: Float,
    val appVersion: String?
)

data class FailurePattern(
    val appId: String,
    val procedureId: String,
    val appVersion: String?,
    val failureType: String,
    val observation: String,
    val recovery: String?,
    val confidence: Float,
    val occurrenceCount: Int,
    val lastSeen: Long
)
