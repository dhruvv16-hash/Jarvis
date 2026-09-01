package com.jarvispoc.agent

enum class PlanStepStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED }

data class PlanStep(
    val stepId: String,
    val description: String,
    val toolId: String,
    val arguments: Map<String, Any>,
    var status: PlanStepStatus,
    val expectedOutcome: String,
    val risk: com.jarvispoc.security.RiskClassification
)

data class Plan(
    val steps: MutableList<PlanStep>
)
