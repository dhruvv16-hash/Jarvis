package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jarvispoc.data.entity.ExecutionRecordEntity
import com.jarvispoc.data.entity.ActionAttemptEntity
import com.jarvispoc.data.entity.ObservationEntity

@Dao
interface ExecutionHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExecution(record: ExecutionRecordEntity)

    @Query("SELECT * FROM execution_history WHERE id = :id")
    fun getExecution(id: String): ExecutionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActionAttempt(attempt: ActionAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertObservation(observation: ObservationEntity)
}

