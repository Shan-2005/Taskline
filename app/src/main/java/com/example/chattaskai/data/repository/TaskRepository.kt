package com.example.chattaskai.data.repository

import com.example.chattaskai.data.database.TaskDao
import com.example.chattaskai.data.database.TaskEntity
import com.example.chattaskai.data.database.SubTaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksByStatus(status: String): Flow<List<TaskEntity>> = taskDao.getTasksByStatus(status)
    suspend fun getTasksByStatusOnce(status: String): List<TaskEntity> = taskDao.getTasksByStatusOnce(status)
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> = taskDao.getTasksByCategory(category)


    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun upsertNotificationTask(task: TaskEntity): Long {
        val existingTask = taskDao.getTaskBySourceSenderAndMessage(task.sourceApp, task.sender, task.originalMessage)
        return if (existingTask != null) {
            val preservedTask = task.copy(
                id = existingTask.id,
                status = existingTask.status,
                createdAt = existingTask.createdAt
            )
            taskDao.updateTask(preservedTask)
            existingTask.id
        } else {
            taskDao.insertTask(task)
        }
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun getTaskById(taskId: Long): TaskEntity? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun getAllTasksOnce(): List<TaskEntity> {
        return taskDao.getAllTasksOnce()
    }

    suspend fun deleteTaskById(taskId: Long) {
        taskDao.deleteSubTasksByTaskId(taskId)
        taskDao.deleteTaskById(taskId)
    }

    suspend fun updateTaskStatus(taskId: Long, status: String) {
        taskDao.updateTaskStatus(taskId, status)
    }

    // Subtask repository methods
    suspend fun insertSubTask(subTask: SubTaskEntity): Long = taskDao.insertSubTask(subTask)
    suspend fun updateSubTask(subTask: SubTaskEntity) = taskDao.updateSubTask(subTask)
    suspend fun deleteSubTaskById(subTaskId: Long) = taskDao.deleteSubTaskById(subTaskId)
    fun getSubTasksForTask(taskId: Long): Flow<List<SubTaskEntity>> = taskDao.getSubTasksForTask(taskId)
    suspend fun getSubTasksForTaskOnce(taskId: Long): List<SubTaskEntity> = taskDao.getSubTasksForTaskOnce(taskId)
}
