package com.jarvispoc.agent

import com.jarvispoc.testdoubles.FakeContextProvider
import com.jarvispoc.testdoubles.FakePlanner
import com.jarvispoc.testdoubles.FakeToolExecutor
import com.jarvispoc.execution.ExecutionResult
import com.jarvispoc.execution.DriverType
import com.jarvispoc.tools.ToolCall
import com.jarvispoc.tools.ToolResultStatus
import com.jarvispoc.perception.Observation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLoopTest {

    @Test
    fun testSuccessfulAgentLoop() = runBlocking {
        val fakeContext = FakeContextProvider()
        val expectedCall = ToolCall("t1", "tool_search", "shopping.search", mapOf("query" to "usb-c"), "task1", "session1")
        
        val fakePlanner = FakePlanner(listOf(
            PlannerDecision.Action(expectedCall),
            PlannerDecision.Complete("Found usb-c cable")
        ))
        
        val fakeExecutor = FakeToolExecutor(listOf(
            ExecutionResult(ToolResultStatus.SUCCESS, "Search completed", driverUsed = DriverType.ACCESSIBILITY, observations = listOf(Observation("Search results loaded", false)))
        ))

        val loop = AgentLoop(fakeContext, fakePlanner, fakeExecutor)

        val goal = Goal("g1", "Search Amazon for USB-C", "Search Amazon for USB-C", emptyMap(), emptyMap(), 0L)
        val request = AgentRequest("r1", "u1", "s1", "t1", goal, "Search Amazon", emptyMap())

        val response = loop.run(request)

        assertEquals(AgentResponseStatus.SUCCESS, response.status)
        assertEquals("Found usb-c cable", response.summary)
        assertEquals(1, fakeExecutor.executedCalls.size)
        assertEquals("tool_search", fakeExecutor.executedCalls[0].toolId)
    }

    @Test
    fun testConfirmationPausesLoop() = runBlocking {
        val fakeContext = FakeContextProvider()
        val expectedCall = ToolCall("t1", "tool_purchase", "shopping.buy", mapOf(), "task1", "session1")
        
        val fakePlanner = FakePlanner(listOf(
            PlannerDecision.Action(expectedCall)
        ))
        
        val fakeExecutor = FakeToolExecutor(listOf(
            ExecutionResult(ToolResultStatus.WAITING_FOR_USER, "Requires Confirmation", driverUsed = DriverType.NATIVE_API)
        ))

        val loop = AgentLoop(fakeContext, fakePlanner, fakeExecutor)
        val goal = Goal("g1", "Buy milk", "Buy milk", emptyMap(), emptyMap(), 0L)
        val request = AgentRequest("r1", "u1", "s1", "t1", goal, "Buy milk", emptyMap())

        val response = loop.run(request)

        assertEquals(AgentResponseStatus.WAITING_FOR_CONFIRMATION, response.status)
        assertEquals(true, response.requiresUserAction)
        assertEquals(1, fakeExecutor.executedCalls.size)
    }

    @Test
    fun testMaxTurnsTimeout() = runBlocking {
        val fakeContext = FakeContextProvider()
        val fakePlanner = FakePlanner(listOf(
            PlannerDecision.Action(ToolCall("t1", "loop_tool", null, mapOf(), "task1", "s1")),
            PlannerDecision.Action(ToolCall("t2", "loop_tool", null, mapOf(), "task1", "s1")),
            PlannerDecision.Action(ToolCall("t3", "loop_tool", null, mapOf(), "task1", "s1")),
            PlannerDecision.Action(ToolCall("t4", "loop_tool", null, mapOf(), "task1", "s1"))
        ))
        
        val fakeExecutor = FakeToolExecutor(listOf(
            ExecutionResult(ToolResultStatus.SUCCESS, "Ok", driverUsed = DriverType.NATIVE_API),
            ExecutionResult(ToolResultStatus.SUCCESS, "Ok", driverUsed = DriverType.NATIVE_API),
            ExecutionResult(ToolResultStatus.SUCCESS, "Ok", driverUsed = DriverType.NATIVE_API),
            ExecutionResult(ToolResultStatus.SUCCESS, "Ok", driverUsed = DriverType.NATIVE_API)
        ))

        val loop = AgentLoop(fakeContext, fakePlanner, fakeExecutor, maxTurns = 3)
        val goal = Goal("g1", "Do something forever", "Loop", emptyMap(), emptyMap(), 0L)
        val request = AgentRequest("r1", "u1", "s1", "t1", goal, "Loop", emptyMap())

        val response = loop.run(request)

        assertEquals(AgentResponseStatus.TIMEOUT, response.status)
        assertEquals(3, fakeExecutor.executedCalls.size)
    }

    @Test
    fun testToolFailureReplan() = runBlocking {
        val fakeContext = FakeContextProvider()
        
        val fakePlanner = FakePlanner(listOf(
            PlannerDecision.Action(ToolCall("t1", "tool_a", null, mapOf(), "task1", "s1")),
            PlannerDecision.Action(ToolCall("t2", "tool_b", null, mapOf(), "task1", "s1")),
            PlannerDecision.Complete("Success on B")
        ))
        
        val fakeExecutor = FakeToolExecutor(listOf(
            ExecutionResult(ToolResultStatus.FAILED, "Retryable Error", driverUsed = DriverType.NATIVE_API, retryable = true),
            ExecutionResult(ToolResultStatus.SUCCESS, "Ok", driverUsed = DriverType.NATIVE_API)
        ))

        val loop = AgentLoop(fakeContext, fakePlanner, fakeExecutor)
        val goal = Goal("g1", "Test", "Test", emptyMap(), emptyMap(), 0L)
        val request = AgentRequest("r1", "u1", "s1", "t1", goal, "Test", emptyMap())

        val response = loop.run(request)

        assertEquals(AgentResponseStatus.SUCCESS, response.status)
        assertEquals(2, fakeExecutor.executedCalls.size)
    }

    @Test
    fun testUnrecoverableFailure() = runBlocking {
        val fakeContext = FakeContextProvider()
        
        val fakePlanner = FakePlanner(listOf(
            PlannerDecision.Action(ToolCall("t1", "tool_c", null, mapOf(), "task1", "s1"))
        ))
        
        val fakeExecutor = FakeToolExecutor(listOf(
            ExecutionResult(ToolResultStatus.FAILED, "Execution Failed", error = "Fatal Error", driverUsed = DriverType.NATIVE_API, retryable = false)
        ))

        val loop = AgentLoop(fakeContext, fakePlanner, fakeExecutor)
        val goal = Goal("g1", "Test", "Test", emptyMap(), emptyMap(), 0L)
        val request = AgentRequest("r1", "u1", "s1", "t1", goal, "Test", emptyMap())

        val response = loop.run(request)

        assertEquals(AgentResponseStatus.FAILED, response.status)
        assertTrue(response.error!!.contains("Fatal Error"))
        assertEquals(1, fakeExecutor.executedCalls.size)
    }

    @Test
    fun testAppRequiredPausesLoop() = runBlocking {
        val fakeContext = FakeContextProvider()
        val expectedCall = ToolCall("t1", "tool_order", "grocery.delivery", mapOf(), "task1", "session1")
        
        val fakePlanner = FakePlanner(listOf(
            PlannerDecision.Action(expectedCall)
        ))
        
        val fakeExecutor = FakeToolExecutor(listOf(
            ExecutionResult(
                status = ToolResultStatus.APP_REQUIRED,
                summary = "Missing App",
                structuredData = mapOf("appId" to "com.blinkit.app"),
                driverUsed = DriverType.NATIVE_API
            )
        ))

        val loop = AgentLoop(fakeContext, fakePlanner, fakeExecutor)
        val goal = Goal("g1", "Order milk", "Order milk", emptyMap(), emptyMap(), 0L)
        val request = AgentRequest("r1", "u1", "s1", "t1", goal, "Order milk", emptyMap())

        val response = loop.run(request)

        assertEquals(AgentResponseStatus.WAITING_FOR_APP_INSTALL, response.status)
        assertEquals(true, response.requiresUserAction)
        assertEquals("com.blinkit.app", response.error)
        assertEquals(1, fakeExecutor.executedCalls.size)
    }
}
