package com.example.chattaskai.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.chattaskai.data.database.AppDatabase
import com.example.chattaskai.data.repository.TaskRepository

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TaskRepository(AppDatabase.getDatabase(appContext).taskDao())

                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    val tasks = repository.getTasksByStatusOnce("pending")
                    tasks.forEach { task ->
                        ReminderManager.scheduleReminder(appContext, task)
                    }
                    return@launch
                }

                val taskId = intent.getLongExtra("TASK_ID", -1)
                val alarmType = intent.getStringExtra("ALARM_TYPE") ?: "FULL"
                if (taskId == -1L) return@launch

                val task = repository.getTaskById(taskId)
                if (task != null && task.status == "pending") {
                    showNotification(appContext, taskId, task.title, alarmType)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, alarmType: String) {
        val isFullAlarm = alarmType == "FULL"
        val isMorning = alarmType == "MORNING"
        val channelId = if (isFullAlarm) "task_alarms_clock" else "task_reminders_subtle"
        val channelName = if (isFullAlarm) "Clock-style Alarms" else "Subtle Reminders"
        val importance = if (isFullAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                if (isFullAlarm) {
                    setSound(
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                } else {
                    setSound(
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                        null
                    )
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, com.example.chattaskai.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_SCREEN", "taskDetail")
            putExtra("TASK_ID", taskId)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            taskId.toInt(),
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isMorning) android.R.drawable.ic_menu_today else android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(when(alarmType) {
                "MORNING" -> "Today's Deadline 📅"
                "HEADS_UP" -> "Reminder (in 15m) ⏳"
                "SUBTLE" -> "Coming up soon (in 5m) ⏳"
                "FULL" -> "TIME IS UP! 🚨"
                else -> "Task Reminder"
            })
            .setContentText(title)
            .setPriority(if (isFullAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(when(alarmType) {
                "FULL" -> NotificationCompat.CATEGORY_ALARM
                "MORNING" -> NotificationCompat.CATEGORY_EVENT
                else -> NotificationCompat.CATEGORY_REMINDER
            })

        if (isFullAlarm) {
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
            builder.setVibrate(longArrayOf(0, 1000, 500, 1000))
        }

        // Add Interactive Actions
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_COMPLETE"
            putExtra("TASK_ID", taskId)
            putExtra("NOTIFICATION_ID", if (isFullAlarm) taskId.toInt() + 10000 else taskId.toInt())
        }
        val completePendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 20000,
            completeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("TASK_ID", taskId)
            putExtra("NOTIFICATION_ID", if (isFullAlarm) taskId.toInt() + 10000 else taskId.toInt())
        }
        val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 30000,
            snoozeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(android.R.drawable.ic_menu_edit, "Snooze (10m)", snoozePendingIntent)
        builder.addAction(android.R.drawable.ic_menu_save, "Mark as Completed", completePendingIntent)

        notificationManager.notify(if (isFullAlarm) taskId.toInt() + 10000 else taskId.toInt(), builder.build())
    }
}
