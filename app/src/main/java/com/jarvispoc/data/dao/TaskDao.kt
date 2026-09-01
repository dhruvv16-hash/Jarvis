package com.jarvispoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jarvispoc.data.entity.TaskEntity
import com.jarvispoc.data.entity.TaskStepEntity

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTask(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStep(step: TaskStepEntity)

    @Query("SELECT * FROM task_steps WHERE taskId = :taskId ORDER BY sequence ASC")
    fun getStepsForTask(taskId: String): List<TaskStepEntity>
}

