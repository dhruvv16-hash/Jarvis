package com.jarvispoc.memory.entity

enum class EntityType {
    PERSON, ORGANIZATION, PLACE, APP, PRODUCT, PROJECT, DEVICE, ACCOUNT, EVENT, TASK
}

data class Entity(
    val entityId: String,
    val type: EntityType,
    val name: String,
    val aliases: List<String>,
    val knownApps: List<String>,
    val knownHandles: List<String>,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Relationship(
    val sourceEntityId: String,
    val targetEntityId: String,
    val type: String, // e.g. "brother_of", "works_at", "uses"
    val confidence: Float,
    val source: String, // e.g. "USER_EXPLICIT", "INFERRED"
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val active: Boolean = true
)

interface EntityManager {
    fun createEntity(entity: Entity)
    fun getEntity(entityId: String): Entity?
    fun createRelationship(relationship: Relationship)
    fun getRelationshipsFor(entityId: String): List<Relationship>
    fun searchEntities(nameOrAlias: String): List<Entity>
}

class DefaultEntityManager : EntityManager {
    private val entities = mutableMapOf<String, Entity>()
    private val relationships = mutableListOf<Relationship>()

    override fun createEntity(entity: Entity) {
        entities[entity.entityId] = entity
    }

    override fun getEntity(entityId: String): Entity? {
        return entities[entityId]?.takeIf { it.active }
    }

    override fun createRelationship(relationship: Relationship) {
        relationships.add(relationship)
    }

    override fun getRelationshipsFor(entityId: String): List<Relationship> {
        return relationships.filter { 
            (it.sourceEntityId == entityId || it.targetEntityId == entityId) && it.active 
        }
    }

    override fun searchEntities(nameOrAlias: String): List<Entity> {
        val query = nameOrAlias.lowercase()
        return entities.values.filter { 
            it.active && (it.name.lowercase().contains(query) || it.aliases.any { alias -> alias.lowercase().contains(query) })
        }
    }
}

interface EntityResolver {
    fun resolve(reference: String, contextAppId: String?): Entity?
}

class DefaultEntityResolver(private val entityManager: EntityManager) : EntityResolver {
    override fun resolve(reference: String, contextAppId: String?): Entity? {
        val matches = entityManager.searchEntities(reference)
        if (matches.size == 1) return matches.first()
        // If ambiguous, return null (implies ASK_USER)
        return null
    }
}
