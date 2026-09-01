package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jarvispoc.data.entity.AppEntity
import com.jarvispoc.data.entity.AppCapabilityEntity
import com.jarvispoc.data.entity.AppDriverEntity
import com.jarvispoc.data.entity.AppStateEntity

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertApp(app: AppEntity)

    @Query("SELECT * FROM apps WHERE id = :id")
    fun getApp(id: String): AppEntity?

    @Query("SELECT * FROM apps")
    fun getAllApps(): List<AppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCapability(capability: AppCapabilityEntity)

    @Query("SELECT * FROM app_capabilities WHERE appId = :appId")
    fun getCapabilities(appId: String): List<AppCapabilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDriver(driver: AppDriverEntity)

    @Query("SELECT * FROM app_drivers WHERE appId = :appId")
    fun getDrivers(appId: String): List<AppDriverEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertState(state: AppStateEntity)

    @Query("SELECT * FROM app_state WHERE appId = :appId")
    fun getState(appId: String): AppStateEntity?
}

