package com.jarvispoc.model

import com.jarvispoc.tools.ToolCall

class FakeLanguageModel(
    private val expectedOutputs: List<ModelDecision>
) : LanguageModel {
    private var callIndex = 0

    override suspend fun generateAction(context: String, availableTools: String): ModelDecision {
        if (callIndex < expectedOutputs.size) {
            return expectedOutputs[callIndex++]
        }
        return ModelDecision.Complete(summary = "Default completion after sequence exhausted.")
    }
}
