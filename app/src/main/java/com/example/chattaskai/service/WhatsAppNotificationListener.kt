package com.example.chattaskai.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.chattaskai.data.database.AppDatabase
import com.example.chattaskai.data.repository.TaskRepository
import com.example.chattaskai.data.profile.ProfileStore

class WhatsAppNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: TaskRepository
    private lateinit var localTaskParser: LocalTaskParser
    private lateinit var profileStore: ProfileStore
    
    // Simple cache to prevent duplicates (Key: sender + text -> Timestamp)
    private val recentMessages = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val CACHE_TTL = 10000L // 10 seconds

    override fun onCreate() {
        super.onCreate()
        val dao = AppDatabase.getDatabase(applicationContext).taskDao()
        repository = TaskRepository(dao)
        localTaskParser = LocalTaskParser()
        profileStore = ProfileStore(applicationContext)
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
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras.getString("android.title")
                ?: "Unknown Sender"

            if (packageName.contains("whatsapp")) {
                profileStore.addKnownWhatsAppSource(title)
            }

            val bodyCandidates = listOfNotNull(
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") { it.toString() }
            ).map { it.trim() }.filter { it.isNotBlank() }

            val body = bodyCandidates.joinToString("\n").trim()
            val normalizedText = if (packageName.contains("mail") || packageName.contains("gm") || packageName.contains("outlook")) {
                listOf(title, body).filter { it.isNotBlank() }.joinToString("\n").trim()
            } else {
                body.ifBlank { title }
            }

            if (normalizedText.isEmpty() || normalizedText == "Checking for new messages") {
                Log.d("WhatsAppListener", "Skipping empty or system notification.")
                return
            }

            // Skip content that is usually metadata noise, not actionable tasks.
            if (normalizedText.length < 8 || normalizedText.startsWith("http", ignoreCase = true)) {
                Log.d("WhatsAppListener", "Skipping low-signal notification payload.")
                return
            }

            // Deduplication logic (Fix for "4 tasks for 1 message")
            val messageKey = "$title|$normalizedText"
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

            Log.d("WhatsAppListener", "Processing Message from $title: $normalizedText")

            // Rebalanced Filter: Tasks are either explicit requests + actions, or actions + timeframes.
            val actionKeywords = listOf("remind", "buy", "call", "send", "submit", "fix", "check", "finish", "complete", "create", "write", "prepare", "report", "document", "todo", "appointment", "meeting", "deadline", "urgent", "review", "update", "cancel", "come", "go", "visit", "attend", "join", "meet", "do", "start", "wrap", "get", "bring", "give", "ask", "tell", "show", "pay", "order", "deliver", "assignment", "submission")
            val requestKeywords = listOf("please", "can you", "could you", "need to", "have to", "must", "make sure", "don't forget", "task:", "todo:", "remind me", "i need", "i want", "kindly", "ensure", "priority", "required")
            val temporalKeywords = listOf("tomorrow", "tonight", "today", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "morning", "evening", "afternoon", "noon", "lunch", "dinner", "eod", "hour", "hr", "min", "month", "week", "daily", "weekly", "monthly", "anytime")
            
            val lowerText = normalizedText.lowercase()
            val hasAction = actionKeywords.any { lowerText.contains(it) }
            val hasRequest = requestKeywords.any { lowerText.contains(it) }
            val hasTimeframe = temporalKeywords.any { lowerText.contains(it) } || normalizedText.contains("\\d{1,2}:\\d{2}|\\d{1,2}\\s*(am|pm|hrs)".toRegex(RegexOption.IGNORE_CASE)) || lowerText.contains(" at ") || lowerText.contains(" by ") || normalizedText.contains("\\d{1,2}/\\d{1,2}".toRegex()) || normalizedText.contains("\\d{4}-\\d{2}-\\d{2}".toRegex())

            val prefs = applicationContext.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val isStrict = prefs.getBoolean("strict_filter", true)
            val trackingSnapshot = profileStore.snapshot()

            if (!NotificationSourceMatcher.shouldTrack(packageName, title, normalizedText, trackingSnapshot)) {
                Log.d("WhatsAppListener", "Notification filtered out by source rules.")
                return
            }

            // Validation Matrix: 
            // 1. Explicitly asked to do an action (e.g., "Please send...")
            // 2. Action bound to a distinct time Constraints (e.g., "Call him tomorrow")
            val isValidTask = if (isStrict) {
                (hasAction && hasRequest) || (hasAction && hasTimeframe)
            } else {
                hasAction
            }

            if (isValidTask) {
                scope.launch {
                    try {
                        val strictParsedTask = localTaskParser.parse(normalizedText, strict = isStrict)
                        val parsedTask = strictParsedTask ?: if (isStrict && hasAction) {
                            localTaskParser.parse(normalizedText, strict = false)
                        } else {
                            null
                        }

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
                                originalMessage = normalizedText,
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
                            val shouldReview = strictParsedTask == null
                            val entityToStore = if (shouldReview) entity.copy(status = "needs_review") else entity
                            val id = repository.upsertNotificationTask(entityToStore)
                            if (!shouldReview) {
                                com.example.chattaskai.reminder.ReminderManager.scheduleReminder(applicationContext, entity.copy(id = id))
                            }
                            Log.d("WhatsAppListener", "Task #$id saved or updated and reminder set.")
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
