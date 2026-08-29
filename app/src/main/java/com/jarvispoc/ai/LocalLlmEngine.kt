package com.jarvispoc.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.jarvispoc.core.AgentLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalLlmEngine(private val context: Context) {
    private val mutex = Mutex()

    @Volatile
    private var engine: LlmInference? = null

    suspend fun reply(incomingMessage: String): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val llm = engine ?: createEngine().also { engine = it }

                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(TOP_K)
                    .setTemperature(TEMPERATURE)
                    .build()

                LlmInferenceSession.createFromOptions(llm, sessionOptions).use { session ->
                    val prompt = buildPrompt(incomingMessage)
                    session.addQueryChunk(prompt)

                    val started = System.currentTimeMillis()
                    val raw = session.generateResponse()
                    AgentLog.info("reply generated in ${System.currentTimeMillis() - started}ms")
                    tidy(raw)
                }
            }.onFailure {
                AgentLog.error("reply failed: ${it.javaClass.simpleName}: ${it.message}")
            }
        }
    }

    private fun createEngine(): LlmInference {
        val model = ModelLocator.resolve(context)
            ?: error("Model not found.")

        AgentLog.info("loading ${model.name} for chat")
        val started = System.currentTimeMillis()

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(model.absolutePath)
            .setMaxTopK(TOP_K)
            .build()

        return LlmInference.createFromOptions(context, options).also {
            AgentLog.success("chat model loaded in ${System.currentTimeMillis() - started}ms")
        }
    }

    private fun buildPrompt(msg: String): String = """
        You are a helpful assistant replying to a message from a friend.
        Friend says: "$msg"
        Write a short, natural, and friendly reply.
        No quotation marks, no preamble. Just the reply.
    """.trimIndent()

    private fun tidy(raw: String): String {
        return raw.substringBefore("<end_of_turn>")
            .substringBefore("<eos>")
            .substringBefore("<start_of_turn>")
            .trim()
            .removeSurrounding("\"")
            .removePrefix("Reply:")
            .trim()
    }

    fun close() {
        if (!mutex.isLocked) {
            runCatching { engine?.close() }
            engine = null
        }
    }

    companion object {
        const val TOP_K = 40
        const val TEMPERATURE = 0.8f
    }
}
