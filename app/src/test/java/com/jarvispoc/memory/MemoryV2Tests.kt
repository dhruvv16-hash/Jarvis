package com.jarvispoc.memory

import com.jarvispoc.memory.entity.*
import com.jarvispoc.memory.preference.*
import com.jarvispoc.memory.retrieval.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MemoryV2Tests {

    @Test
    fun testEntityResolution() {
        val manager = DefaultEntityManager()
        manager.createEntity(Entity("e1", EntityType.PERSON, "Rahul", listOf("Rahu"), emptyList(), emptyList()))
        
        val resolver = DefaultEntityResolver(manager)
        
        val resolved = resolver.resolve("Rahul", null)
        assertNotNull(resolved)
        assertEquals("e1", resolved?.entityId)
        
        val resolvedAlias = resolver.resolve("Rahu", null)
        assertNotNull(resolvedAlias)
        assertEquals("e1", resolvedAlias?.entityId)
    }

    @Test
    fun testAmbiguousEntityResolution() {
        val manager = DefaultEntityManager()
        manager.createEntity(Entity("e1", EntityType.PERSON, "Rahul", emptyList(), emptyList(), emptyList()))
        manager.createEntity(Entity("e2", EntityType.PERSON, "Rahul Sharma", emptyList(), emptyList(), emptyList()))
        
        val resolver = DefaultEntityResolver(manager)
        
        // "Rahul" matches both "Rahul" and "Rahul Sharma" (as it contains the query)
        val resolved = resolver.resolve("Rahul", null)
        assertNull("Ambiguous resolution must return null (ASK_USER)", resolved)
    }

    @Test
    fun testExplicitPreferenceOverwritesAndRetrieves() {
        val manager = DefaultPreferenceManager()
        val pref1 = Preference(
            "p1", com.jarvispoc.memory.preference.MemoryCategory.PREFERENCE, MemoryScope.GLOBAL, null, "grocery.buy", "Amul milk", 1.0f,
            mutableListOf(MemoryEvidence("ev1", MemorySource.USER_EXPLICIT, "User said prefer amul", System.currentTimeMillis()))
        )
        manager.addPreference(pref1)
        
        val context = manager.getRelevantPreferences(MemoryScope.GLOBAL, null, "grocery.buy")
        assertEquals(1, context.size)
        assertEquals("Amul milk", context[0].value)
        
        // User changes preference
        manager.forgetPreference("p1")
        val pref2 = Preference(
            "p2", com.jarvispoc.memory.preference.MemoryCategory.PREFERENCE, MemoryScope.GLOBAL, null, "grocery.buy", "Mother Dairy", 1.0f,
            mutableListOf(MemoryEvidence("ev2", MemorySource.USER_EXPLICIT, "User said prefer mother dairy", System.currentTimeMillis()))
        )
        manager.addPreference(pref2)
        
        val context2 = manager.getRelevantPreferences(MemoryScope.GLOBAL, null, "grocery.buy")
        assertEquals(1, context2.size)
        assertEquals("Mother Dairy", context2[0].value)
    }

    @Test
    fun testTemporaryOverride() {
        val manager = DefaultPreferenceManager()
        val prefGlobal = Preference(
            "p1", com.jarvispoc.memory.preference.MemoryCategory.PREFERENCE, MemoryScope.GLOBAL, null, "grocery.buy", "Blinkit", 1.0f, mutableListOf()
        )
        manager.addPreference(prefGlobal)
        
        val prefTemp = Preference(
            "p2", com.jarvispoc.memory.preference.MemoryCategory.PREFERENCE, MemoryScope.TEMPORARY, "com.zepto", "grocery.buy", "Zepto", 1.0f, mutableListOf(),
            validUntil = System.currentTimeMillis() + 10000 // 10 seconds
        )
        manager.addPreference(prefTemp)
        
        val retrievalEngine = DefaultMemoryRetrievalEngine(DefaultEntityManager(), manager)
        val context = retrievalEngine.retrieveContext("Order milk", "com.zepto", "grocery.buy")
        
        assertEquals(1, context.temporaryConstraints.size)
        assertEquals("Zepto", context.temporaryConstraints[0].value)
        assertEquals(1, context.preferences.size)
        assertEquals("Blinkit", context.preferences[0].value)
        // Agent logic will apply TEMPORARY > GLOBAL at planning time.
    }
    
    @Test
    fun testRelationships() {
        val manager = DefaultEntityManager()
        manager.createEntity(Entity("e1", EntityType.PERSON, "Rahul", emptyList(), emptyList(), emptyList()))
        manager.createRelationship(Relationship("user_self", "e1", "brother_of", 1.0f, "USER_EXPLICIT"))
        
        val rels = manager.getRelationshipsFor("e1")
        assertEquals(1, rels.size)
        assertEquals("brother_of", rels[0].type)
    }
}
