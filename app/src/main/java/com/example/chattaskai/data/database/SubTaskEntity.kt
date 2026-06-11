package com.example.chattaskai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subtasks")
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false
)
