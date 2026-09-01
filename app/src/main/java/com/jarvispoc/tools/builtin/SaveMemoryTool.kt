package com.jarvispoc.tools.builtin

import com.jarvispoc.tools.*
import com.jarvispoc.security.RiskClassification
import com.jarvispoc.memory.MemoryManager
import com.jarvispoc.memory.MemoryCategory
import com.jarvispoc.memory.preference.MemoryScope
import com.jarvispoc.memory.preference.MemorySource
import com.jarvispoc.agent.RequestSource

class SaveMemoryTool(
    private val memoryManager: MemoryManager
) : Tool {
    override val id: String = "save_memory"
    override val name: String = "Save Memory Tool"
    override val description: String = "Persists a validated user preference to memory."
    override val capabilities: Set<String> = setOf("memory.write")
    override val riskLevel: RiskClassification = RiskClassification.LOW

    override suspend fun execute(call: ToolCall): ToolResult {
        val categoryStr = call.arguments["category"] as? String ?: return fail(call, "Missing category")
        val content = call.arguments["content"] as? String ?: return fail(call, "Missing content")
        val scopeStr = call.arguments["scope"] as? String ?: return fail(call, "Missing scope")
        val sourceStr = call.arguments["source"] as? String ?: return fail(call, "Missing source")
        
        val category = try { MemoryCategory.valueOf(categoryStr) } catch (e: Exception) { return fail(call, "Invalid category: categoryStr") }
        val scope = try { MemoryScope.valueOf(scopeStr) } catch (e: Exception) { return fail(call, "Invalid scope: scopeStr") }
        val source = try { MemorySource.valueOf(sourceStr) } catch (e: Exception) { return fail(call, "Invalid source: sourceStr") }

        val confidence = (call.arguments["confidence"] as? Number)?.toFloat() ?: 1.0f

        // 20. USER EXPLICIT SOURCE VALIDATION
        if (source == MemorySource.USER_EXPLICIT) {
            val requestSourceStr = call.arguments["requestSource"] as? String
            if (requestSourceStr != RequestSource.USER_DIRECT.name && requestSourceStr != RequestSource.USER_VOICE.name) {
                 return fail(call, "Security Violation: Cannot create USER_EXPLICIT memory from non-user provenance.")
            }
        }

        try {
            val appId = (call.arguments["appId"] as? String)?.takeIf { it.isNotBlank() }
            val capId = (call.arguments["capabilityId"] as? String)?.takeIf { it.isNotBlank() }
            
            val id = memoryManager.remember(
                content = content,
                category = category,
                scope = scope,
                source = source,
                appId = appId,
                capabilityId = capId,
                confidence = confidence
            )
            return ToolResult(
                toolCallId = call.id,
                status = ToolResultStatus.SUCCESS,
                summary = "Preference saved: content\nApplies to: scopeStr",
                structuredData = mapOf("preferenceId" to id)
            )
        } catch(e: Exception) {
            return fail(call, "Memory write failed. {e.message}")
        }
    }

    private fun fail(call: ToolCall, msg: String): ToolResult {
        return ToolResult(
            toolCallId = call.id,
            status = ToolResultStatus.FAILED,
            summary = msg,
            error = msg
        )
    }
}
