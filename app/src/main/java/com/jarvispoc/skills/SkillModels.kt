package com.jarvispoc.skills

import java.util.UUID

enum class SkillRiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class Skill(
    val id: String,
    val name: String,
    val appId: String,
    val capabilityId: String,
    val requiredConditions: List<String>,
    val successCriteria: List<String>,
    val riskLevel: SkillRiskLevel,
    val procedureId: String,
    val version: String,
    val confidence: Float
)

interface SkillRegistry {
    suspend fun register(skill: Skill)
    suspend fun findByCapability(capabilityId: String): List<Skill>
    suspend fun getAppSkills(appId: String): List<Skill>
}

class DefaultSkillRegistry : SkillRegistry {
    private val skills = mutableListOf<Skill>()

    override suspend fun register(skill: Skill) {
        skills.add(skill)
    }

    override suspend fun findByCapability(capabilityId: String): List<Skill> {
        return skills.filter { it.capabilityId == capabilityId }
    }

    override suspend fun getAppSkills(appId: String): List<Skill> {
        return skills.filter { it.appId == appId }
    }
}

interface SkillGenerator {
    suspend fun generateSkill(
        appId: String,
        capabilityId: String,
        procedureId: String,
        riskLevel: SkillRiskLevel
    ): Skill
}

class DefaultSkillGenerator : SkillGenerator {
    override suspend fun generateSkill(
        appId: String,
        capabilityId: String,
        procedureId: String,
        riskLevel: SkillRiskLevel
    ): Skill {
        return Skill(
            id = UUID.randomUUID().toString(),
            name = "Generated Skill for $capabilityId",
            appId = appId,
            capabilityId = capabilityId,
            requiredConditions = listOf("App Installed", "Target Screen Reached"),
            successCriteria = listOf("Verification Node Visible"),
            riskLevel = riskLevel,
            procedureId = procedureId,
            version = "1.0",
            confidence = 0.5f
        )
    }
}
