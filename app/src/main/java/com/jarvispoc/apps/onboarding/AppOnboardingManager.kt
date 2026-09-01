package com.jarvispoc.apps.onboarding

interface AppOnboardingManager {
    suspend fun onboardApp(
        appId: String, 
        taskId: String, 
        sessionId: String, 
        goal: String, 
        budget: ExplorationBudget
    ): AppOnboardingResult
}

class DefaultAppOnboardingManager(
    private val inspectionEngine: AppInspectionEngine,
    private val discoveryEngine: CapabilityDiscoveryEngine,
    private val hypothesisRepo: HypothesisRepository
) : AppOnboardingManager {
    
    override suspend fun onboardApp(
        appId: String, 
        taskId: String, 
        sessionId: String, 
        goal: String, 
        budget: ExplorationBudget
    ): AppOnboardingResult {
        val session = AppOnboardingSession(
            id = "sess_${System.currentTimeMillis()}",
            appId = appId,
            taskId = taskId,
            sessionId = sessionId,
            goal = goal,
            status = OnboardingSessionStatus.STARTED,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            actionsUsed = 0,
            screensVisited = 0,
            capabilitiesDiscovered = mutableListOf()
        )
        
        session.status = OnboardingSessionStatus.INSPECTING
        
        // 1. Inspect
        val elements = inspectionEngine.inspectApp(appId)
        val screenRole = inspectionEngine.classifyScreen(elements)
        session.screensVisited++

        if (screenRole == ScreenRole.LOGIN || screenRole == ScreenRole.PERMISSION) {
            session.status = OnboardingSessionStatus.FAILED
            return AppOnboardingResult(
                status = AppOnboardingResultStatus.REQUIRES_USER,
                capabilitiesDiscovered = emptyList(),
                proceduresLearned = emptyList(),
                limitations = listOf("Authentication or Permission required"),
                nextAction = "Prompt User for Auth/Permissions"
            )
        }

        session.status = OnboardingSessionStatus.DISCOVERING
        
        // 2. Discover
        val hypotheses = discoveryEngine.discoverCapabilities(appId, elements, goal)
        hypotheses.forEach { hypothesisRepo.save(it) }

        if (hypotheses.isEmpty()) {
            session.status = OnboardingSessionStatus.FAILED
            return AppOnboardingResult(
                status = AppOnboardingResultStatus.UNSUPPORTED,
                capabilitiesDiscovered = emptyList(),
                proceduresLearned = emptyList(),
                limitations = listOf("No capabilities discovered matching the goal."),
                nextAction = null
            )
        }
        
        session.status = OnboardingSessionStatus.TESTING
        
        // 3. Test (Simulated by verifying one hypothesis for proof-of-concept)
        val testedCapabilities = mutableListOf<String>()
        val learnedProcedures = mutableListOf<String>()
        
        for (hypothesis in hypotheses) {
            if (session.actionsUsed >= budget.maxActions) break
            
            // Simulating execution and success verification
            session.actionsUsed++
            val testedHypothesis = hypothesis.copy(status = HypothesisStatus.VERIFIED)
            hypothesisRepo.save(testedHypothesis)
            testedCapabilities.add(testedHypothesis.capabilityId)
            session.capabilitiesDiscovered.add(testedHypothesis.capabilityId)
            learnedProcedures.add("Procedure for ${testedHypothesis.capabilityId}")
        }
        
        session.status = OnboardingSessionStatus.COMPLETED
        session.completedAt = System.currentTimeMillis()
        
        return AppOnboardingResult(
            status = AppOnboardingResultStatus.SUCCESS,
            capabilitiesDiscovered = testedCapabilities,
            proceduresLearned = learnedProcedures,
            limitations = emptyList(),
            nextAction = "Ready for Planner"
        )
    }
}

interface HypothesisRepository {
    fun save(hypothesis: CapabilityHypothesis)
    fun getByApp(appId: String): List<CapabilityHypothesis>
}
