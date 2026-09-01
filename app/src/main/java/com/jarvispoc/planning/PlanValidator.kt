package com.jarvispoc.planning

class PlanValidator {
    
    fun validateGraph(graph: TaskGraph, maxTasks: Int = 50): Boolean {
        if (graph.tasks.size > maxTasks) return false
        
        // Cycle detection via DFS
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        fun hasCycle(taskId: String): Boolean {
            if (recursionStack.contains(taskId)) return true
            if (visited.contains(taskId)) return false
            
            visited.add(taskId)
            recursionStack.add(taskId)
            
            val task = graph.tasks[taskId] ?: return false // Missing dependency node
            for (dep in task.dependencies) {
                if (hasCycle(dep)) return true
            }
            
            recursionStack.remove(taskId)
            return false
        }
        
        for (taskId in graph.tasks.keys) {
            if (!visited.contains(taskId)) {
                if (hasCycle(taskId)) return false
            }
        }
        
        return true
    }
}

interface PlanScheduler {
    fun getNextDispatchableTasks(graph: TaskGraph): List<PlanTask>
    fun markTaskComplete(graph: TaskGraph, taskId: String)
    fun markTaskFailed(graph: TaskGraph, taskId: String)
}

class DefaultPlanScheduler : PlanScheduler {
    
    override fun getNextDispatchableTasks(graph: TaskGraph): List<PlanTask> {
        val ready = graph.getReadyTasks()
        ready.forEach { it.status = TaskStatus.READY }
        return ready
    }

    override fun markTaskComplete(graph: TaskGraph, taskId: String) {
        graph.tasks[taskId]?.status = TaskStatus.COMPLETED
    }

    override fun markTaskFailed(graph: TaskGraph, taskId: String) {
        graph.tasks[taskId]?.status = TaskStatus.FAILED
    }
}
