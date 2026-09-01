package com.jarvispoc.planning

interface StrategicPlanner {
    fun generatePlan(objective: Objective): TaskGraph
    fun replan(objective: Objective, currentGraph: TaskGraph): TaskGraph
}

class FakeStrategicPlanner : StrategicPlanner {
    override fun generatePlan(objective: Objective): TaskGraph {
        val tasks = mutableMapOf<String, PlanTask>()
        
        // Simple synthetic plan: A -> B
        val taskA = PlanTask("tA", objective.id, "Research phase", null, TaskStatus.PENDING)
        val taskB = PlanTask("tB", objective.id, "Execution phase", null, TaskStatus.PENDING, dependencies = mutableListOf("tA"))
        
        tasks["tA"] = taskA
        tasks["tB"] = taskB
        
        return TaskGraph(objective.id, 1, tasks)
    }

    override fun replan(objective: Objective, currentGraph: TaskGraph): TaskGraph {
        val newTasks = currentGraph.tasks.toMutableMap()
        val taskC = PlanTask("tC", objective.id, "Recovery phase", null, TaskStatus.PENDING)
        newTasks["tC"] = taskC
        return TaskGraph(objective.id, currentGraph.version + 1, newTasks)
    }
}

class ObjectiveVerifier {
    fun verify(objective: Objective, graph: TaskGraph): ObjectiveStatus {
        val allTasks = graph.tasks.values
        if (allTasks.all { it.status == TaskStatus.COMPLETED }) {
            return ObjectiveStatus.COMPLETED
        }
        
        val anyFailed = allTasks.any { it.status == TaskStatus.FAILED }
        val someCompleted = allTasks.any { it.status == TaskStatus.COMPLETED }
        
        if (anyFailed && someCompleted) {
            return ObjectiveStatus.PARTIALLY_COMPLETED
        } else if (anyFailed) {
            return ObjectiveStatus.FAILED
        }
        
        return ObjectiveStatus.EXECUTING
    }
}
