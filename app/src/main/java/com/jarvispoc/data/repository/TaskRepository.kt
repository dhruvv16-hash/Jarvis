package com.jarvispoc.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.jarvispoc.data.dao.TaskDao
import com.jarvispoc.data.entity.TaskEntity
import com.jarvispoc.data.entity.TaskStepEntity

class TaskRepository(private val taskDao: TaskDao) {
    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) { taskDao.insertTask(task) }
    suspend fun getTask(id: String) = withContext(Dispatchers.IO) { taskDao.getTask(id) }
    suspend fun insertStep(step: TaskStepEntity) = withContext(Dispatchers.IO) { taskDao.insertStep(step) }
    suspend fun getStepsForTask(taskId: String) = withContext(Dispatchers.IO) { taskDao.getStepsForTask(taskId) }
}

