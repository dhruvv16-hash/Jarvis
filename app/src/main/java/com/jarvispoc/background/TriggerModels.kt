package com.jarvispoc.background

import com.jarvispoc.events.JarvisEvent

enum class TriggerType {
    ONE_TIME, SCHEDULED, INTERVAL, APP_EVENT, NOTIFICATION, 
    DEVICE_EVENT, LOCATION, TASK_COMPLETION, USER_EVENT
}

data class TriggerCondition(
    val key: String,
    val operator: String,
    val value: String
)

data class Trigger(
    val id: String,
    val type: TriggerType,
    val enabled: Boolean,
    val conditions: List<TriggerCondition>,
    val taskIdTemplate: String
)

interface ConditionEvaluator {
    fun evaluate(conditions: List<TriggerCondition>, event: JarvisEvent): Boolean
}

class DefaultConditionEvaluator : ConditionEvaluator {
    override fun evaluate(conditions: List<TriggerCondition>, event: JarvisEvent): Boolean {
        for (condition in conditions) {
            when (condition.key) {
                "appId" -> if (event.appId != condition.value) return false
                "senderId" -> if (event.senderId != condition.value) return false
            }
        }
        return true
    }
}

interface TriggerManager {
    fun evaluateEvent(event: JarvisEvent): List<Trigger>
    fun registerTrigger(trigger: Trigger)
}

class DefaultTriggerManager(
    private val evaluator: ConditionEvaluator
) : TriggerManager {
    private val triggers = mutableListOf<Trigger>()
    
    override fun registerTrigger(trigger: Trigger) {
        triggers.add(trigger)
    }
    
    override fun evaluateEvent(event: JarvisEvent): List<Trigger> {
        return triggers.filter { it.enabled && evaluator.evaluate(it.conditions, event) }
    }
}
