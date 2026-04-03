package com.example.chattaskai.data.repository

import com.example.chattaskai.data.database.TaskDao
import com.example.chattaskai.data.database.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksByStatus(status: String): Flow<List<TaskEntity>> = taskDao.getTasksByStatus(status)
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> = taskDao.getTasksByCategory(category)


    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun getTaskById(taskId: Long): TaskEntity? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun deleteTaskById(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun updateTaskStatus(taskId: Long, status: String) {
        taskDao.updateTaskStatus(taskId, status)
    }
}
