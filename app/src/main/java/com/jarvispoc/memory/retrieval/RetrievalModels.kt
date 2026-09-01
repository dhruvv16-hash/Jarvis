package com.jarvispoc.memory.retrieval

import com.jarvispoc.memory.entity.Entity
import com.jarvispoc.memory.entity.EntityManager
import com.jarvispoc.memory.entity.Relationship
import com.jarvispoc.memory.preference.Preference
import com.jarvispoc.memory.preference.PreferenceManager
import com.jarvispoc.memory.preference.MemoryScope

data class PersonalContext(
    val userProfile: String,
    val people: List<Entity>,
    val relationships: List<Relationship>,
    val preferences: List<Preference>,
    val relevantExperiences: List<String>,
    val temporaryConstraints: List<Preference>
)

interface MemoryRetrievalEngine {
    fun retrieveContext(goal: String, appId: String?, capabilityId: String?): PersonalContext
}

class DefaultMemoryRetrievalEngine(
    private val entityManager: EntityManager,
    private val preferenceManager: PreferenceManager
) : MemoryRetrievalEngine {
    
    override fun retrieveContext(goal: String, appId: String?, capabilityId: String?): PersonalContext {
        // 1. Entity Extraction (Simulated)
        val extractedEntities = mutableListOf<Entity>()
        val words = goal.split(" ")
        for (word in words) {
            val entities = entityManager.searchEntities(word)
            extractedEntities.addAll(entities)
        }
        val uniqueEntities = extractedEntities.distinctBy { it.entityId }

        // 2. Relationships
        val relationships = uniqueEntities.flatMap { entityManager.getRelationshipsFor(it.entityId) }.distinct()

        // 3. Preferences (App & Global & Capability)
        val allPrefs = preferenceManager.getRelevantPreferences(MemoryScope.APP, appId, capabilityId)
        val temporaryConstraints = allPrefs.filter { it.scope == MemoryScope.TEMPORARY }
        val stablePreferences = allPrefs.filter { it.scope != MemoryScope.TEMPORARY }

        return PersonalContext(
            userProfile = "User likes concise automation.",
            people = uniqueEntities.filter { it.type == com.jarvispoc.memory.entity.EntityType.PERSON },
            relationships = relationships,
            preferences = stablePreferences,
            relevantExperiences = emptyList(),
            temporaryConstraints = temporaryConstraints
        )
    }
}
