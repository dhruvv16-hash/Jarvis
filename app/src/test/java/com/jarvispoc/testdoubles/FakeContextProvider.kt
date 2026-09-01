package com.jarvispoc.testdoubles

import com.jarvispoc.agent.AgentContextProvider
import com.jarvispoc.agent.AgentRequest

class FakeContextProvider(
    var contextToReturn: String = "Fake Context"
) : AgentContextProvider {
    override suspend fun buildContext(request: AgentRequest, observationSummary: String?): String {
        return contextToReturn + if (observationSummary != null) "\nObservation: $observationSummary" else ""
    }
}
