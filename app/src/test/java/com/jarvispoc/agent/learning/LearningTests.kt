package com.jarvispoc.agent.learning

import com.jarvispoc.agent.Goal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LearningTests {

    private val fakeRepo = object : ProcedureRepository {
        val procedures = mutableListOf<Procedure>()
        override suspend fun findByGoalAndApp(goal: String, appId: String) = procedures.filter { it.goalDescription == goal && it.appId == appId }
        override suspend fun findByCapability(capabilityId: String, appId: String) = procedures.filter { it.capabilityId == capabilityId && it.appId == appId }
        override suspend fun save(procedure: Procedure) { procedures.add(procedure) }
        override suspend fun markNeedsValidation(appId: String, newAppVersion: String) {}
    }

    private val fakeUiRepo = object : UiKnowledgeRepository {
        val knowledgeList = mutableListOf<UiKnowledge>()
        override suspend fun findByRole(appId: String, role: String) = knowledgeList.filter { it.appId == appId && it.semanticRole == role }
        override suspend fun save(knowledge: UiKnowledge) { knowledgeList.add(knowledge) }
    }

    private val fakeFailureRepo = object : FailureRepository {
        val failures = mutableListOf<FailurePattern>()
        override suspend fun save(pattern: FailurePattern) { failures.add(pattern) }
    }

    @Test
    fun testProcedureMatcher() = runBlocking {
        fakeRepo.procedures.clear()
        fakeRepo.save(Procedure("p1", "Old Search", "search", "app1", "cap1", ProcedureType.LEARNED, listOf(ProcedureStep(1, "tap", mapOf(), null, null, null)), "1.0", 0.5f, 1, 0, null, null))
        fakeRepo.save(Procedure("p2", "Manual Search", "search", "app1", "cap1", ProcedureType.MANUAL, listOf(ProcedureStep(1, "tap", mapOf(), null, null, null)), "1.0", 0.9f, 5, 0, null, null))
        
        val matcher = DefaultProcedureMatcher(fakeRepo)
        val matches = matcher.matchProcedures(Goal("g1", "search", "search", emptyMap(), emptyMap(), 0L), "app1", "cap1")
        
        assertEquals(2, matches.size)
        assertEquals("p2", matches[0].id) // Manual should be ranked higher due to 100f override
    }

    @Test
    fun testSemanticMatcher() = runBlocking {
        fakeUiRepo.knowledgeList.clear()
        fakeUiRepo.save(UiKnowledge("app1", "home", "search_box", "Find items", "tap", 0.9f, "1.0"))

        val matcher = DefaultSemanticMatcher(fakeUiRepo)
        
        val candidates = matcher.findTarget(
            mapOf("semanticRole" to "search_box"),
            "Screen contains: Find items and menu",
            "app1",
            "1.0"
        )
        
        assertTrue(candidates.isNotEmpty())
        assertEquals("Find items", candidates[0].nodeId)
        assertEquals("learned_knowledge", candidates[0].source)
    }

    @Test
    fun testProcedureLearnerSuccess() = runBlocking {
        fakeRepo.procedures.clear()
        val learner = DefaultProcedureLearner(fakeRepo, fakeFailureRepo)
        
        learner.observeSuccess("search", "app1", "cap1", listOf(ProcedureStep(1, "tap", mapOf(), null, null, null)), "1.0")
        
        assertEquals(1, fakeRepo.procedures.size)
        val p = fakeRepo.procedures[0]
        assertEquals(ProcedureType.LEARNED, p.type)
        assertEquals(0.5f, p.confidence)
        assertEquals(1, p.successCount)
    }

    @Test
    fun testProcedureLearnerFailure() = runBlocking {
        fakeFailureRepo.failures.clear()
        val learner = DefaultProcedureLearner(fakeRepo, fakeFailureRepo)
        
        learner.observeFailure("p1", "app1", "1.0", "Element not found", "Home screen")
        
        assertEquals(1, fakeFailureRepo.failures.size)
        assertEquals("Element not found", fakeFailureRepo.failures[0].failureType)
    }

    @Test
    fun testAdaptationEngine() = runBlocking {
        val engine = DefaultAdaptationEngine()
        val oldProcedure = Procedure("p1", "Old", "search", "app1", "cap1", ProcedureType.LEARNED, emptyList(), "1.0", 0.9f, 10, 0, null, null)
        
        val adapted = engine.adaptProcedure(oldProcedure, "New UI element appeared", "Element not found")
        
        assertNotNull(adapted)
        assertNotEquals("p1", adapted!!.id)
        assertEquals(ProcedureType.ADAPTED, adapted.type)
        assertEquals(0.3f, adapted.confidence)
    }
}
