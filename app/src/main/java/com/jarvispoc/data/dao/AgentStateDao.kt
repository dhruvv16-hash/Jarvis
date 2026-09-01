package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jarvispoc.data.entity.AgentStateEntity

@Dao
interface AgentStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateState(state: AgentStateEntity)

    @Query("SELECT * FROM agent_state WHERE id = 1")
    fun getState(): AgentStateEntity?
}

