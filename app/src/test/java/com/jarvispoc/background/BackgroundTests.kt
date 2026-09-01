package com.jarvispoc.background

import com.jarvispoc.automation.*
import com.jarvispoc.events.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackgroundTests {

    @Test
    fun testEventDeduplication() {
        val deduplicator = DefaultEventDeduplicator()
        val event1 = JarvisEvent("e1", "hash_123", JarvisEventType.NOTIFICATION_RECEIVED, 0L, "com.telegram", "Rahul", emptyMap())
        val event2 = JarvisEvent("e2", "hash_123", JarvisEventType.NOTIFICATION_RECEIVED, 0L, "com.telegram", "Rahul", emptyMap())
        
        assertFalse(deduplicator.isDuplicate(event1))
        assertTrue(deduplicator.isDuplicate(event2))
    }

    @Test
    fun testEventTriggersAutomation() = runBlocking {
        val conditionEvaluator = DefaultConditionEvaluator()
        val triggerManager = DefaultTriggerManager(conditionEvaluator)
        val automationManager = DefaultAutomationManager()
        val agent = FakeBackgroundAgent()
        val dispatcher = DefaultTaskDispatcher(agent)
        
        val trigger = Trigger("t1", TriggerType.NOTIFICATION, true, listOf(
            TriggerCondition("appId", "==", "com.telegram"),
            TriggerCondition("senderId", "==", "Rahul")
        ), "t_template_1")
        
        triggerManager.registerTrigger(trigger)
        
        val automation = Automation("a1", "Reply to Rahul", "Drafts reply", "Summarize and draft reply", "t1", AutomationStatus.ACTIVE, "LOW", 3)
        automationManager.createAutomation(automation)
        
        val event = JarvisEvent("e1", "h1", JarvisEventType.NOTIFICATION_RECEIVED, 0L, "com.telegram", "Rahul", emptyMap())
        
        // Pipeline
        val matchedTriggers = triggerManager.evaluateEvent(event)
        assertEquals(1, matchedTriggers.size)
        
        val result = dispatcher.dispatch(matchedTriggers[0], automation, event)
        assertEquals("COMPLETED", result)
    }

    @Test
    fun testAutomationPaused() = runBlocking {
        val conditionEvaluator = DefaultConditionEvaluator()
        val triggerManager = DefaultTriggerManager(conditionEvaluator)
        val agent = FakeBackgroundAgent()
        val dispatcher = DefaultTaskDispatcher(agent)
        
        val trigger = Trigger("t1", TriggerType.NOTIFICATION, true, emptyList(), "t_template_1")
        triggerManager.registerTrigger(trigger)
        
        val automation = Automation("a1", "Paused Auto", "Paused", "Do something", "t1", AutomationStatus.PAUSED, "LOW", 3)
        
        val event = JarvisEvent("e1", "h1", JarvisEventType.NOTIFICATION_RECEIVED, 0L, "app", null, emptyMap())
        
        val matchedTriggers = triggerManager.evaluateEvent(event)
        val result = dispatcher.dispatch(matchedTriggers[0], automation, event)
        
        assertEquals("SKIPPED_PAUSED", result)
    }

    @Test
    fun testHighRiskTaskRequiresUser() = runBlocking {
        val conditionEvaluator = DefaultConditionEvaluator()
        val triggerManager = DefaultTriggerManager(conditionEvaluator)
        val agent = FakeBackgroundAgent()
        val dispatcher = DefaultTaskDispatcher(agent)
        
        val trigger = Trigger("t1", TriggerType.SCHEDULED, true, emptyList(), "t_template_1")
        triggerManager.registerTrigger(trigger)
        
        val automation = Automation("a1", "High Risk Task", "Requires auth", "HIGH_RISK: Transfer money", "t1", AutomationStatus.ACTIVE, "HIGH", 3)
        
        val event = JarvisEvent("e1", "h1", JarvisEventType.TIME_SCHEDULE_MATCH, 0L, null, null, emptyMap())
        
        val matchedTriggers = triggerManager.evaluateEvent(event)
        val result = dispatcher.dispatch(matchedTriggers[0], automation, event)
        
        assertEquals("WAITING_FOR_USER_CONFIRMATION", result)
    }
}
