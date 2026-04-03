package com.example.chattaskai.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.chattaskai.data.database.AppDatabase
import com.example.chattaskai.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val taskId = intent.getLongExtra("TASK_ID", -1)
        val action = intent.action

        if (taskId == -1L) {
            pendingResult.finish()
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", taskId.toInt())
        notificationManager.cancel(notificationId)

        val repository = TaskRepository(AppDatabase.getDatabase(context).taskDao())
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            "ACTION_COMPLETE" -> {
                Log.d("ActionReceiver", "Marking task $taskId as completed")
                scope.launch {
                    try {
                        val task = repository.getTaskById(taskId)
                        if (task != null) {
                            repository.updateTaskStatus(taskId, "completed")
                            ReminderManager.cancelReminder(context, taskId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "ACTION_SNOOZE" -> {
                Log.d("ActionReceiver", "Snoozing task $taskId")
                scope.launch {
                    try {
                        val task = repository.getTaskById(taskId)
                        if (task != null) {
                            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                            val snoozeMin = prefs.getInt("snooze_min", 10)

                            // Reschedule as a FULL alarm in X minutes
                            val snoozeTime = System.currentTimeMillis() + (snoozeMin * 60 * 1000)

                            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                                putExtra("TASK_ID", taskId)
                                putExtra("TASK_TITLE", task.title)
                                putExtra("ALARM_TYPE", "FULL")
                            }

                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                context.applicationContext,
                                (taskId * 4 + 3).toInt(), // Use the FULL alarm request code
                                snoozeIntent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                            )

                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                            } else {
                                alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> pendingResult.finish()
        }
    }
}
