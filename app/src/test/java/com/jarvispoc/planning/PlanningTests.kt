package com.jarvispoc.planning

import org.junit.Assert.*
import org.junit.Test

class PlanningTests {

    @Test
    fun testDependencyAndScheduler() {
        val objective = Objective("obj1", "user1", "Trip", "NORMAL", ObjectiveStatus.PLANNING)
        val planner = FakeStrategicPlanner()
        val graph = planner.generatePlan(objective) // Creates tA -> tB
        
        val scheduler = DefaultPlanScheduler()
        
        // Initial state: tA should be READY, tB should be PENDING (blocked by tA)
        var readyTasks = scheduler.getNextDispatchableTasks(graph)
        assertEquals(1, readyTasks.size)
        assertEquals("tA", readyTasks[0].id)
        
        // Complete tA
        scheduler.markTaskComplete(graph, "tA")
        
        // Next state: tB should now be READY
        readyTasks = scheduler.getNextDispatchableTasks(graph)
        assertEquals(1, readyTasks.size)
        assertEquals("tB", readyTasks[0].id)
    }

    @Test
    fun testParallelism() {
        val graph = TaskGraph("obj1", 1, mutableMapOf(
            "tA" to PlanTask("tA", "obj1", "Parallel 1", null, TaskStatus.PENDING),
            "tB" to PlanTask("tB", "obj1", "Parallel 2", null, TaskStatus.PENDING)
        ))
        val scheduler = DefaultPlanScheduler()
        val readyTasks = scheduler.getNextDispatchableTasks(graph)
        
        assertEquals(2, readyTasks.size) // Both should be ready since neither have dependencies
    }

    @Test
    fun testCycleDetectionRejectsGraph() {
        val validator = PlanValidator()
        val graph = TaskGraph("obj1", 1, mutableMapOf(
            "tA" to PlanTask("tA", "obj1", "A", null, TaskStatus.PENDING, dependencies = mutableListOf("tB")),
            "tB" to PlanTask("tB", "obj1", "B", null, TaskStatus.PENDING, dependencies = mutableListOf("tA"))
        ))
        
        val isValid = validator.validateGraph(graph)
        assertFalse("Graph with cycle must be rejected", isValid)
    }

    @Test
    fun testResourceLimit() {
        val validator = PlanValidator()
        val manyTasks = (1..51).associate { 
            "t$it" to PlanTask("t$it", "obj", "desc", null) 
        }.toMutableMap()
        
        val graph = TaskGraph("obj", 1, manyTasks)
        val isValid = validator.validateGraph(graph, maxTasks = 50)
        assertFalse("Graph exceeding resource limit must be rejected", isValid)
    }

    @Test
    fun testPartialCompletion() {
        val graph = TaskGraph("obj1", 1, mutableMapOf(
            "tA" to PlanTask("tA", "obj1", "A", null, TaskStatus.COMPLETED),
            "tB" to PlanTask("tB", "obj1", "B", null, TaskStatus.COMPLETED),
            "tC" to PlanTask("tC", "obj1", "C", null, TaskStatus.FAILED)
        ))
        
        val verifier = ObjectiveVerifier()
        val objective = Objective("obj1", "user1", "Trip", "NORMAL", ObjectiveStatus.EXECUTING)
        
        val finalStatus = verifier.verify(objective, graph)
        assertEquals(ObjectiveStatus.PARTIALLY_COMPLETED, finalStatus)
    }

    @Test
    fun testUserInputBlock() {
        val task = PlanTask("tA", "obj", "A", null, TaskStatus.BLOCKED, TaskBlockReason.USER_INPUT)
        assertEquals(TaskStatus.BLOCKED, task.status)
        assertEquals(TaskBlockReason.USER_INPUT, task.blockReason)
    }

    @Test
    fun testReplanning() {
        val objective = Objective("obj1", "user1", "Trip", "NORMAL", ObjectiveStatus.EXECUTING)
        val planner = FakeStrategicPlanner()
        val initialGraph = planner.generatePlan(objective)
        
        val replannedGraph = planner.replan(objective, initialGraph)
        assertEquals(2, replannedGraph.version)
        assertTrue(replannedGraph.tasks.containsKey("tC")) // Recovery phase added
    }

    @Test
    fun testCancellation() {
        val objective = Objective("obj1", "u1", "obj", "NORMAL", ObjectiveStatus.EXECUTING)
        objective.status = ObjectiveStatus.CANCELLED
        
        // Simulating the cancellation event updating all pending tasks
        val tasks = listOf(
            PlanTask("tA", "obj1", "A", null, TaskStatus.RUNNING),
            PlanTask("tB", "obj1", "B", null, TaskStatus.PENDING)
        )
        
        tasks.forEach { 
            if(it.status == TaskStatus.PENDING) it.status = TaskStatus.CANCELLED 
        }
        
        assertEquals(TaskStatus.RUNNING, tasks[0].status)
        assertEquals(TaskStatus.CANCELLED, tasks[1].status)
    }
}
