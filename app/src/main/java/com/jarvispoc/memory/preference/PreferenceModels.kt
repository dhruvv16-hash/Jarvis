package com.jarvispoc.memory.preference

enum class MemoryScope {
    GLOBAL, APP, CAPABILITY, PERSON, TASK, SESSION, TEMPORARY
}

enum class MemorySource {
    USER_EXPLICIT, USER_BEHAVIOR, APP_OBSERVATION, EXECUTION_RESULT, PROCEDURE_LEARNING, SYSTEM_DERIVED
}

enum class MemoryCategory {
    SEMANTIC, EPISODIC, PREFERENCE, INSTRUCTION, RELATIONSHIP, USER_PROFILE, PROCEDURAL, APP_KNOWLEDGE, APP_STATE, SESSION_CONTEXT
}

data class MemoryEvidence(
    val evidenceId: String,
    val source: MemorySource,
    val detail: String,
    val timestamp: Long
)

data class Preference(
    val preferenceId: String,
    val category: MemoryCategory,
    val scope: MemoryScope,
    val targetAppId: String?,
    val targetCapabilityId: String?,
    val value: String, // e.g. "Amul milk", "concise"
    var confidence: Float,
    val evidence: MutableList<MemoryEvidence>,
    val validUntil: Long? = null,
    var active: Boolean = true,
    val isNegative: Boolean = false
)

interface PreferenceManager {
    fun addPreference(preference: Preference)
    fun getRelevantPreferences(scope: MemoryScope, appId: String?, capabilityId: String?): List<Preference>
    fun forgetPreference(preferenceId: String)
}

class DefaultPreferenceManager : PreferenceManager {
    private val preferences = mutableListOf<Preference>()

    override fun addPreference(preference: Preference) {
        val existing = preferences.find { it.preferenceId == preference.preferenceId }
        if (existing != null) {
            existing.confidence = preference.confidence
            existing.evidence.addAll(preference.evidence)
            existing.active = preference.active
        } else {
            preferences.add(preference)
        }
    }

    override fun getRelevantPreferences(scope: MemoryScope, appId: String?, capabilityId: String?): List<Preference> {
        val now = System.currentTimeMillis()
        return preferences.filter {
            it.active &&
            (it.validUntil == null || it.validUntil > now) &&
            (it.scope == MemoryScope.GLOBAL || it.scope == scope || it.targetAppId == appId || it.targetCapabilityId == capabilityId)
        }.sortedByDescending { it.confidence }
    }

    override fun forgetPreference(preferenceId: String) {
        preferences.find { it.preferenceId == preferenceId }?.active = false
    }
}
