package com.jarvispoc.events

enum class JarvisEventType {
    APP_INSTALLED, APP_UNINSTALLED, PACKAGE_UPDATED, NOTIFICATION_RECEIVED,
    USER_MESSAGE, TASK_COMPLETED, TASK_FAILED, DEVICE_BOOTED, NETWORK_CHANGED,
    TIME_SCHEDULE_MATCH
}

data class JarvisEvent(
    val eventId: String,
    val dedupeKey: String,
    val type: JarvisEventType,
    val timestamp: Long,
    val appId: String?,
    val senderId: String?,
    val metadata: Map<String, String>
)

interface EventDeduplicator {
    fun isDuplicate(event: JarvisEvent): Boolean
}

class DefaultEventDeduplicator : EventDeduplicator {
    private val seenKeys = mutableSetOf<String>()
    
    override fun isDuplicate(event: JarvisEvent): Boolean {
        if (seenKeys.contains(event.dedupeKey)) return true
        seenKeys.add(event.dedupeKey)
        // Keep bounded size in real impl
        return false
    }
}
