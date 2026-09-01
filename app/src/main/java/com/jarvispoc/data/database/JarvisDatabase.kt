package com.jarvispoc.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jarvispoc.data.converter.Converters
import com.jarvispoc.data.dao.*
import com.jarvispoc.data.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        MemoryEntity::class,
        MemoryFtsEntity::class,
        ProcedureEntity::class,
        AppEntity::class,
        AppCapabilityEntity::class,
        AppDriverEntity::class,
        AppStateEntity::class,
        ExecutionRecordEntity::class,
        ActionAttemptEntity::class,
        ObservationEntity::class,
        SessionEntity::class,
        TaskEntity::class,
        TaskStepEntity::class,
        AgentStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun memoryDao(): MemoryDao
    abstract fun procedureDao(): ProcedureDao
    abstract fun appDao(): AppDao
    abstract fun executionHistoryDao(): ExecutionHistoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun taskDao(): TaskDao
    abstract fun agentStateDao(): AgentStateDao
}
