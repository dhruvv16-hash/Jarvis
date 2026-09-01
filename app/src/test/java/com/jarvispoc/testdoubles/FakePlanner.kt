package com.jarvispoc.testdoubles

import com.jarvispoc.agent.AgentPlanner
import com.jarvispoc.agent.AgentRequest
import com.jarvispoc.agent.Goal
import com.jarvispoc.agent.PlannerDecision
import com.jarvispoc.tools.ToolCall

class FakePlanner(
    private val decisions: List<PlannerDecision>
) : AgentPlanner {
    private var callIndex = 0

    override suspend fun decideNextAction(context: String, goal: Goal, sessionId: String, taskId: String?): PlannerDecision {
        if (callIndex < decisions.size) {
            return decisions[callIndex++]
        }
        return PlannerDecision.Complete("Sequence exhausted.")
    }
}
