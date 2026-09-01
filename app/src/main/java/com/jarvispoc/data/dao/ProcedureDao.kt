package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jarvispoc.data.entity.ProcedureEntity

@Dao
interface ProcedureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(procedure: ProcedureEntity)

    @Query("SELECT * FROM procedures WHERE id = :id")
    fun getById(id: String): ProcedureEntity?

    @Query("SELECT * FROM procedures WHERE appId = :appId")
    fun getByApp(appId: String): List<ProcedureEntity>
}

