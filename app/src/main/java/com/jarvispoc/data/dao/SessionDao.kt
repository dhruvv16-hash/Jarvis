package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jarvispoc.data.entity.SessionEntity

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' ORDER BY lastActiveAt DESC LIMIT 1")
    fun getActiveSession(): SessionEntity?
}

