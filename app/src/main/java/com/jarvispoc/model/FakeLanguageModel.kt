package com.jarvispoc.model

import com.jarvispoc.tools.ToolCall

class FakeLanguageModel(
    private val expectedOutputs: List<ModelOutput>
) : LanguageModel {
    private var callIndex = 0

    override suspend fun generateAction(context: String, availableTools: String): ModelOutput {
        if (callIndex < expectedOutputs.size) {
            return expectedOutputs[callIndex++]
        }
        return ModelOutput("complete", summary = "Default completion after sequence exhausted.")
    }
}
