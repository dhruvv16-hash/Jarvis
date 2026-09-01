package com.jarvispoc.tools

interface ToolRegistry {
    fun register(tool: Tool)
    fun unregister(tool: Tool)
    fun get(toolId: String): Tool?
    fun findByCapability(capabilityId: String): List<Tool>
    fun list(): List<Tool>
    fun contains(toolId: String): Boolean
}

class DefaultToolRegistry : ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()
    override fun register(tool: Tool) { tools[tool.id] = tool }
    override fun unregister(tool: Tool) { tools.remove(tool.id) }
    override fun get(toolId: String): Tool? = tools[toolId]
    override fun findByCapability(capabilityId: String): List<Tool> = tools.values.filter { it.capabilities.contains(capabilityId) }
    override fun list(): List<Tool> = tools.values.toList()
    override fun contains(toolId: String): Boolean = tools.containsKey(toolId)
}
