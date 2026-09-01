package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jarvispoc.data.entity.MemoryEntity
import com.jarvispoc.memory.MemoryCategory

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(memory: MemoryEntity)

    @Query("SELECT * FROM memories WHERE id = :id")
    fun getById(id: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY updatedAt DESC")
    fun getByCategory(category: MemoryCategory): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE appId = :appId ORDER BY updatedAt DESC")
    fun getByApp(appId: String): List<MemoryEntity>

    @Query("SELECT * FROM memories JOIN memories_fts ON memories.id = memories_fts.rowid WHERE memories_fts MATCH :query ORDER BY updatedAt DESC")
    fun search(query: String): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE id = :id")
    fun deleteById(id: String)

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): List<MemoryEntity>
}

