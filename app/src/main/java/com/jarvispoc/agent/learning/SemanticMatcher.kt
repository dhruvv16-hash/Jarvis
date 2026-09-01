package com.jarvispoc.agent.learning

data class SemanticCandidate(
    val nodeId: String,
    val score: Float,
    val source: String
)

interface SemanticMatcher {
    suspend fun findTarget(
        targetDescription: Map<String, String>, 
        currentObservation: String, 
        appId: String, 
        appVersion: String?
    ): List<SemanticCandidate>
}

class DefaultSemanticMatcher(
    private val uiKnowledgeRepo: UiKnowledgeRepository
) : SemanticMatcher {
    override suspend fun findTarget(
        targetDescription: Map<String, String>, 
        currentObservation: String, 
        appId: String, 
        appVersion: String?
    ): List<SemanticCandidate> {
        val candidates = mutableListOf<SemanticCandidate>()
        
        // 1. Exact match from target params
        val exactId = targetDescription["resourceId"]
        if (exactId != null && currentObservation.contains(exactId)) {
            candidates.add(SemanticCandidate(exactId, 1.0f, "exact_id"))
        }

        // 2. Semantic Role lookup from learned UI knowledge
        val role = targetDescription["semanticRole"]
        if (role != null) {
            val knowledge = uiKnowledgeRepo.findByRole(appId, role)
            knowledge.forEach { k ->
                if (currentObservation.contains(k.observedText)) {
                    candidates.add(SemanticCandidate(k.observedText, k.confidence, "learned_knowledge"))
                }
            }
        }

        // 3. Fallback ranking based on text match
        val text = targetDescription["text"]
        if (text != null && currentObservation.contains(text)) {
            candidates.add(SemanticCandidate(text, 0.5f, "text_match"))
        }
        
        return candidates.sortedByDescending { it.score }
    }
}

interface UiKnowledgeRepository {
    suspend fun findByRole(appId: String, role: String): List<UiKnowledge>
    suspend fun save(knowledge: UiKnowledge)
}
