package com.jarvispoc.apps.onboarding

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import com.jarvispoc.skills.*

class OnboardingTests {

    private val fakeHypothesisRepo = object : HypothesisRepository {
        val hypotheses = mutableListOf<CapabilityHypothesis>()
        override fun save(hypothesis: CapabilityHypothesis) {
            hypotheses.removeIf { it.id == hypothesis.id }
            hypotheses.add(hypothesis)
        }
        override fun getByApp(appId: String) = hypotheses.filter { it.appId == appId }
    }

    private val fakeInspectionEngine = object : AppInspectionEngine {
        override suspend fun inspectApp(appId: String): List<UiElement> {
            if (appId == "com.fake.grocery") {
                return listOf(
                    UiElement("search_box", "search", "Find groceries", "Search products", null, null, true, true, true, false, 1.0f),
                    UiElement("cart_btn", "button", "Cart", "View Cart", null, null, true, true, false, false, 1.0f)
                )
            } else if (appId == "com.fake.authapp") {
                return listOf(
                    UiElement("login_btn", "button", "Login", null, null, null, true, true, false, false, 1.0f)
                )
            }
            return emptyList()
        }

        override suspend fun classifyScreen(elements: List<UiElement>): ScreenRole {
            if (elements.any { it.text == "Login" }) return ScreenRole.LOGIN
            if (elements.any { it.role == "search" }) return ScreenRole.HOME
            return ScreenRole.UNKNOWN
        }
    }

    @Test
    fun testAppOnboardingSuccess() = runBlocking {
        val discoveryEngine = DefaultCapabilityDiscoveryEngine()
        val manager = DefaultAppOnboardingManager(fakeInspectionEngine, discoveryEngine, fakeHypothesisRepo)
        
        val result = manager.onboardApp(
            "com.fake.grocery",
            "task_1",
            "sess_1",
            "Search for milk",
            ExplorationBudget()
        )
        
        assertEquals(AppOnboardingResultStatus.SUCCESS, result.status)
        assertTrue(result.capabilitiesDiscovered.contains("grocery.search"))
        
        val hypotheses = fakeHypothesisRepo.getByApp("com.fake.grocery")
        assertTrue(hypotheses.any { it.capabilityId == "grocery.search" && it.status == HypothesisStatus.VERIFIED })
    }

    @Test
    fun testAppOnboardingRequiresAuth() = runBlocking {
        val discoveryEngine = DefaultCapabilityDiscoveryEngine()
        val manager = DefaultAppOnboardingManager(fakeInspectionEngine, discoveryEngine, fakeHypothesisRepo)
        
        val result = manager.onboardApp(
            "com.fake.authapp",
            "task_2",
            "sess_2",
            "Check balance",
            ExplorationBudget()
        )
        
        assertEquals(AppOnboardingResultStatus.REQUIRES_USER, result.status)
        assertTrue(result.limitations.contains("Authentication or Permission required"))
    }

    @Test
    fun testSkillGeneration() = runBlocking {
        val generator = DefaultSkillGenerator()
        val registry = DefaultSkillRegistry()
        
        val skill = generator.generateSkill("com.fake.grocery", "grocery.search", "proc_123", SkillRiskLevel.LOW)
        registry.register(skill)
        
        val foundSkills = registry.findByCapability("grocery.search")
        assertEquals(1, foundSkills.size)
        assertEquals("proc_123", foundSkills[0].procedureId)
    }
}
