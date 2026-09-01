package com.jarvispoc.planning

enum class ObjectiveStatus {
    PENDING, PLANNING, EXECUTING, WAITING, PAUSED, COMPLETED, PARTIALLY_COMPLETED, FAILED, CANCELLED, EXPIRED, NEEDS_VERIFICATION
}

data class Objective(
    val id: String,
    val userId: String,
    val description: String,
    val priority: String, // LOW, NORMAL, HIGH, URGENT
    var status: ObjectiveStatus,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var progress: Float = 0f
)

enum class TaskStatus {
    PENDING, READY, RUNNING, BLOCKED, WAITING, COMPLETED, FAILED, CANCELLED
}

enum class TaskBlockReason {
    NONE, DEPENDENCY, USER_INPUT, APP_INSTALL, AUTHENTICATION, PERMISSION, DEVICE_BUSY, NETWORK, POLICY
}

data class PlanTask(
    val id: String,
    val objectiveId: String,
    val description: String,
    val capability: String?,
    var status: TaskStatus = TaskStatus.PENDING,
    var blockReason: TaskBlockReason = TaskBlockReason.NONE,
    val dependencies: MutableList<String> = mutableListOf(),
    val constraints: Map<String, String> = emptyMap(),
    val riskLevel: String = "LOW"
)

data class TaskGraph(
    val objectiveId: String,
    var version: Int,
    val tasks: MutableMap<String, PlanTask>
) {
    fun getReadyTasks(): List<PlanTask> {
        return tasks.values.filter { task -> 
            task.status == TaskStatus.PENDING || task.status == TaskStatus.READY 
        }.filter { task ->
            // Ready if all dependencies are COMPLETED
            task.dependencies.all { depId -> tasks[depId]?.status == TaskStatus.COMPLETED }
        }
    }
}
