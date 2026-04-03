package com.example.chattaskai.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.chattaskai.data.database.TaskEntity
import java.util.Calendar

object ReminderManager {

    fun scheduleReminder(context: Context, task: TaskEntity) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val baseIntent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }

        // 1. Morning Reminder (on the day of the deadline)
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val morningHour = prefs.getInt("morning_hour", 9)
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.deadlineTimestamp
            set(Calendar.HOUR_OF_DAY, morningHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val morningTime = calendar.timeInMillis
        // Only schedule if the deadline hasn't passed and morning time is in the future
        if (morningTime > System.currentTimeMillis() && morningTime < task.deadlineTimestamp) {
            val morningIntent = Intent(baseIntent).apply { putExtra("ALARM_TYPE", "MORNING") }
            val morningPendingIntent = PendingIntent.getBroadcast(
                appContext,
                (task.id * 4).toInt(),
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(appContext, alarmManager, morningTime, morningPendingIntent)
        }

        // 2. Schedule Heads-up Reminder (15 mins before)
        val headsUpTime = task.deadlineTimestamp - (15 * 60 * 1000)
        if (headsUpTime > System.currentTimeMillis()) {
            val headsUpIntent = Intent(baseIntent).apply { putExtra("ALARM_TYPE", "HEADS_UP") }
            val headsUpPendingIntent = PendingIntent.getBroadcast(
                appContext,
                (task.id * 4 + 1).toInt(),
                headsUpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(appContext, alarmManager, headsUpTime, headsUpPendingIntent)
        }

        // 3. Schedule Subtle Reminder (5 mins before)
        val subtleTime = task.deadlineTimestamp - (5 * 60 * 1000)
        if (subtleTime > System.currentTimeMillis()) {
            val subtleIntent = Intent(baseIntent).apply { putExtra("ALARM_TYPE", "SUBTLE") }
            val subtlePendingIntent = PendingIntent.getBroadcast(
                appContext,
                (task.id * 4 + 2).toInt(),
                subtleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(appContext, alarmManager, subtleTime, subtlePendingIntent)
        }

        // 4. Schedule Main Alarm (Exactly at deadline)
        val exactTime = task.deadlineTimestamp
        if (exactTime > System.currentTimeMillis()) {
            val fullIntent = Intent(baseIntent).apply { putExtra("ALARM_TYPE", "FULL") }
            val fullPendingIntent = PendingIntent.getBroadcast(
                appContext,
                (task.id * 4 + 3).toInt(),
                fullIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(appContext, alarmManager, exactTime, fullPendingIntent)
        }
    }

    private fun setAlarm(context: Context, alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, ReminderReceiver::class.java)
        
        // Cancel all four
        listOf(taskId * 4, taskId * 4 + 1, taskId * 4 + 2, taskId * 4 + 3).forEach { id ->
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
