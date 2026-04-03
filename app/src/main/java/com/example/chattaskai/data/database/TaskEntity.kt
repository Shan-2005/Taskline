package com.example.chattaskai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalMessage: String,
    val sender: String,
    val deadlineDate: String, // YYYY-MM-DD format
    val deadlineTime: String, // HH:MM format
    val deadlineTimestamp: Long, // Unix timestamp for easy sorting/alarms
    val priority: String, // "high", "medium", "low"
    val category: String = "General", // "Work", "Personal", etc.
    val reminderMinutesBefore: Int, // 10, 60, 1440
    val sourceApp: String = "WhatsApp",
    val status: String = "pending", // "pending", "completed"
    val createdAt: Long = System.currentTimeMillis()
)
