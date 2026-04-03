package com.example.chattaskai.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.chattaskai.data.database.AppDatabase
import com.example.chattaskai.data.repository.TaskRepository

class WhatsAppNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: TaskRepository
    private lateinit var localTaskParser: LocalTaskParser
    
    // Simple cache to prevent duplicates (Key: sender + text -> Timestamp)
    private val recentMessages = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val CACHE_TTL = 10000L // 10 seconds

    override fun onCreate() {
        super.onCreate()
        val dao = AppDatabase.getDatabase(applicationContext).taskDao()
        repository = TaskRepository(dao)
        localTaskParser = LocalTaskParser()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val supportedApps = listOf("com.whatsapp", "com.google.android.gm", "com.microsoft.office.outlook")
        
        if (supportedApps.contains(packageName)) {
            Log.d("WhatsAppListener", "Supported notification detected: $packageName")
            
            // Security: Ignore summary/group notifications (Vulnerability Fix)
            val isSummary = (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
            if (isSummary) {
                Log.d("WhatsAppListener", "Skipping summary/group notification.")
                return
            }

            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: "Unknown Sender"
            
            // Gmail/Outlook often has subject in title and body in text
            val subject = extras.getCharSequence("android.title") ?: ""
            val body = extras.getCharSequence("android.text") ?: ""
            
            val text = if (packageName.contains("mail") || packageName.contains("gm") || packageName.contains("outlook")) {
                "$subject: $body"
            } else {
                body.toString()
            }

            if (text.isEmpty() || text == "Checking for new messages") {
                Log.d("WhatsAppListener", "Skipping empty or system notification.")
                return
            }

            // Deduplication logic (Fix for "4 tasks for 1 message")
            val messageKey = "$title|$text"
            val currentTime = System.currentTimeMillis()
            val lastProcessed = recentMessages[messageKey] ?: 0L
            if (currentTime - lastProcessed < CACHE_TTL) {
                Log.d("WhatsAppListener", "Duplicate message detected within TTL. Skipping.")
                return
            }
            recentMessages[messageKey] = currentTime
            
            // Cleanup cache periodically
            if (recentMessages.size > 50) {
                val it = recentMessages.entries.iterator()
                while (it.hasNext()) {
                    if (currentTime - it.next().value > CACHE_TTL) it.remove()
                }
            }

            Log.d("WhatsAppListener", "Processing Message from $title: $text")

            // Rebalanced Filter: Tasks are either explicit requests + actions, or actions + timeframes.
            val actionKeywords = listOf("remind", "buy", "call", "send", "submit", "fix", "check", "finish", "complete", "create", "write", "prepare", "report", "document", "todo", "appointment", "meeting", "deadline", "urgent", "review", "update", "cancel", "come", "go", "visit", "attend", "join", "meet", "do", "start", "wrap", "get", "bring", "give", "ask", "tell", "show", "pay", "order", "deliver", "assignment", "submission")
            val requestKeywords = listOf("please", "can you", "could you", "need to", "have to", "must", "make sure", "don't forget", "task:", "todo:", "remind me", "i need", "i want", "kindly", "ensure", "priority", "required")
            val temporalKeywords = listOf("tomorrow", "tonight", "today", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "morning", "evening", "afternoon", "noon", "lunch", "dinner", "eod", "hour", "hr", "min", "month", "week", "daily", "weekly", "monthly", "anytime")
            
            val lowerText = text.lowercase()
            val hasAction = actionKeywords.any { lowerText.contains(it) }
            val hasRequest = requestKeywords.any { lowerText.contains(it) }
            val hasTimeframe = temporalKeywords.any { lowerText.contains(it) } || text.contains("\\d{1,2}:\\d{2}|\\d{1,2}\\s*(am|pm|hrs)".toRegex(RegexOption.IGNORE_CASE)) || lowerText.contains(" at ") || lowerText.contains(" by ") || text.contains("\\d{1,2}/\\d{1,2}".toRegex()) || text.contains("\\d{4}-\\d{2}-\\d{2}".toRegex())

            // Validation Matrix: 
            // 1. Explicitly asked to do an action (e.g., "Please send...")
            // 2. Action bound to a distinct time Constraints (e.g., "Call him tomorrow")
            val isValidTask = (hasAction && hasRequest) || (hasAction && hasTimeframe)

            if (isValidTask) {
                scope.launch {
                    try {
                        val prefs = applicationContext.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                        val isStrict = prefs.getBoolean("strict_filter", true)
                        val parsedTask = localTaskParser.parse(text, strict = isStrict)

                        if (parsedTask != null && parsedTask.is_task) {
                            Log.d("WhatsAppListener", "SUCCESS: Task found - ${parsedTask.task}")
                            
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            val deadlineTimestamp = try {
                                sdf.parse("${parsedTask.date} ${parsedTask.time}")?.time ?: (System.currentTimeMillis() + 86400000)
                            } catch (e: Exception) {
                                System.currentTimeMillis() + 86400000
                            }

                            val entity = com.example.chattaskai.data.database.TaskEntity(
                                title = parsedTask.task,
                                originalMessage = text,
                                sender = title,
                                deadlineDate = parsedTask.date,
                                deadlineTime = parsedTask.time,
                                deadlineTimestamp = deadlineTimestamp,
                                priority = parsedTask.priority,
                                category = parsedTask.category,
                                reminderMinutesBefore = 60,
                                sourceApp = when {
                                    packageName.contains("whatsapp") -> "WhatsApp"
                                    packageName.contains("gm") -> "Gmail"
                                    packageName.contains("outlook") -> "Outlook"
                                    else -> "Email"
                                }
                            )
                            val id = repository.insertTask(entity)
                            com.example.chattaskai.reminder.ReminderManager.scheduleReminder(applicationContext, entity.copy(id = id))
                            Log.d("WhatsAppListener", "Task #$id saved and reminder set.")
                        }
                    } catch (e: Exception) {
                        Log.e("WhatsAppListener", "Parsing Error: ${e.message}")
                    }
                }
            } else {
                Log.d("WhatsAppListener", "Message length or keyword filter failed.")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not required for now
    }
}
