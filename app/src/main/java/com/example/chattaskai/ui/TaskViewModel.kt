package com.example.chattaskai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.chattaskai.BuildConfig
import com.example.chattaskai.data.database.TaskEntity
import com.example.chattaskai.data.database.SubTaskEntity
import com.example.chattaskai.data.profile.ProfileStore
import com.example.chattaskai.reminder.ReminderManager
import com.example.chattaskai.service.ApkUpdateInfo
import com.example.chattaskai.service.GitHubApkUpdateChecker
import com.example.chattaskai.service.SupabaseSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.chattaskai.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Quote(val text: String, val author: String)
data class ChatMessage(val sender: String, val text: String, val timestamp: Long = System.currentTimeMillis())

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _dailyQuote = MutableStateFlow(Quote("Small steps every day lead to big results.", "TASKLINE"))
    val dailyQuote = _dailyQuote.asStateFlow()

    init {
        fetchRandomQuote()
    }

    private fun fetchRandomQuote() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://zenquotes.io/api/random")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val qStart = response.indexOf("\"q\":\"") + 5
                    val qEnd = response.indexOf("\"", qStart)
                    val aStart = response.indexOf("\"a\":\"") + 5
                    val aEnd = response.indexOf("\"", aStart)
                    
                    if (qStart > 4 && qEnd > qStart && aStart > 4 && aEnd > aStart) {
                        val text = response.substring(qStart, qEnd).replace("\\\"", "\"").replace("\\n", "\n")
                        val author = response.substring(aStart, aEnd)
                        _dailyQuote.value = Quote(text, author)
                    } else {
                        throw Exception("Parse failed")
                    }
                } else {
                    throw Exception("API Error")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbacks = listOf(
                    Quote("The secret of getting ahead is getting started.", "Mark Twain"),
                    Quote("It always seems impossible until it's done.", "Nelson Mandela"),
                    Quote("Don't watch the clock; do what it does. Keep going.", "Sam Levenson"),
                    Quote("The future depends on what you do today.", "Mahatma Gandhi"),
                    Quote("Believe you can and you're halfway there.", "Theodore Roosevelt")
                )
                _dailyQuote.value = fallbacks.random()
            }
        }
    }

    val pendingTasks: StateFlow<List<TaskEntity>> = repository.getTasksByStatus("pending")
        .map { it.sortedBy { task -> task.deadlineTimestamp } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<TaskEntity>> = repository.getTasksByStatus("completed")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviewTasks: StateFlow<List<TaskEntity>> = repository.getTasksByStatus("needs_review")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeHue = MutableStateFlow(0f)
    val themeHue = _themeHue.asStateFlow()

    private val _morningReminderHour = MutableStateFlow(9)
    val morningReminderHour = _morningReminderHour.asStateFlow()

    private val _snoozeMinutes = MutableStateFlow(10)
    val snoozeMinutes = _snoozeMinutes.asStateFlow()

    private val _strictFiltering = MutableStateFlow(true)
    val strictFiltering = _strictFiltering.asStateFlow()

    private val _useGeminiParser = MutableStateFlow(false)
    val useGeminiParser = _useGeminiParser.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey = _geminiApiKey.asStateFlow()

    private val _availableApkUpdate = MutableStateFlow<ApkUpdateInfo?>(null)
    val availableApkUpdate = _availableApkUpdate.asStateFlow()

    private val _syncStatus = MutableStateFlow("Not synced yet")
    val syncStatus = _syncStatus.asStateFlow()

    private val _xp = MutableStateFlow(0)
    val xp = _xp.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level = _level.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak = _streak.asStateFlow()

    private val _levelUpAlert = MutableStateFlow<String?>(null)
    val levelUpAlert = _levelUpAlert.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Taskline AI", "Hello! I am your Taskline AI assistant. You can ask me to add, complete, delete, or list tasks!")
    ))
    val chatMessages = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    fun dismissLevelUpAlert() {
        _levelUpAlert.value = null
    }

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _themeHue.value = prefs.getFloat("theme_hue", 0f)
        _morningReminderHour.value = prefs.getInt("morning_hour", 9)
        _snoozeMinutes.value = prefs.getInt("snooze_min", 10)
        _strictFiltering.value = prefs.getBoolean("strict_filter", true)
        val defaultKey = try {
            if (BuildConfig.GEMINI_API_KEY_B64.isNotBlank()) {
                String(java.util.Base64.getDecoder().decode(BuildConfig.GEMINI_API_KEY_B64), Charsets.UTF_8)
            } else ""
        } catch (e: Exception) {
            ""
        }
        _useGeminiParser.value = prefs.getBoolean("use_gemini_parser", defaultKey.isNotBlank())
        _geminiApiKey.value = prefs.getString("gemini_api_key", "").let {
            if (it.isNullOrBlank()) defaultKey else it
        }
        
        // Load productivity stats
        val profileStore = ProfileStore(context)
        _xp.value = profileStore.getXp()
        _level.value = profileStore.getLevel()
        _streak.value = profileStore.getStreak()
    }

    fun setThemeHue(context: Context, hue: Float) {
        _themeHue.value = hue
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putFloat("theme_hue", hue).apply()
    }

    fun setMorningHour(context: Context, hour: Int) {
        _morningReminderHour.value = hour
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("morning_hour", hour).apply()
    }

    fun setSnoozeMinutes(context: Context, min: Int) {
        _snoozeMinutes.value = min
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("snooze_min", min).apply()
    }

    fun setStrictFiltering(context: Context, enabled: Boolean) {
        _strictFiltering.value = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("strict_filter", enabled).apply()
    }

    fun setUseGeminiParser(context: Context, enabled: Boolean) {
        _useGeminiParser.value = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("use_gemini_parser", enabled).apply()
    }

    fun setGeminiApiKey(context: Context, key: String) {
        _geminiApiKey.value = key
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("gemini_api_key", key).apply()
    }

    fun checkForApkUpdate(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _availableApkUpdate.value = GitHubApkUpdateChecker.findAvailableUpdate(
                context = context,
                repoOwner = BuildConfig.APK_UPDATE_REPO_OWNER,
                repoName = BuildConfig.APK_UPDATE_REPO_NAME,
                assetPrefix = BuildConfig.APK_UPDATE_ASSET_PREFIX
            )
        }
    }

    fun dismissApkUpdateCard() {
        _availableApkUpdate.value = null
    }

    fun syncWithSupabase(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val profileStore = ProfileStore(context)
            val syncService = SupabaseSyncService(repository, profileStore)
            _syncStatus.value = "Syncing with Supabase..."

            try {
                val result = syncService.syncNow()
                _syncStatus.value = "${result.message}: pushed ${result.pushed}, pulled ${result.pulled}"
            } catch (e: Exception) {
                _syncStatus.value = "Supabase sync failed: ${e.message ?: "Unknown error"}"
            }
        }
    }

    fun approveReviewTask(context: Context, task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task.copy(status = "pending"))
            ReminderManager.scheduleReminder(context, task.copy(status = "pending"))
        }
    }

    fun dismissReviewTask(context: Context, task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderManager.cancelReminder(context, task.id)
            repository.deleteTaskById(task.id)
        }
    }


    fun completeTask(context: Context, task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderManager.cancelReminder(context, task.id)
            repository.updateTask(task.copy(status = "completed"))
            awardTaskCompletionXp(context, task)
        }
    }

    private fun awardTaskCompletionXp(context: Context, task: TaskEntity) {
        val profileStore = ProfileStore(context)
        val xpToAward = when (task.priority.lowercase()) {
            "high" -> 30
            "medium" -> 20
            else -> 10
        }
        
        val currentXp = profileStore.getXp()
        val nextXp = currentXp + xpToAward
        profileStore.setXp(nextXp)
        
        val currentLevel = profileStore.getLevel()
        val nextLevel = (nextXp / 100) + 1
        if (nextLevel > currentLevel) {
            profileStore.setLevel(nextLevel)
            _levelUpAlert.value = "Level Up! You reached level $nextLevel!"
        }
        
        val lastCompletionDate = profileStore.getLastCompletionDate()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())
        
        val currentStreak = profileStore.getStreak()
        if (lastCompletionDate.isBlank()) {
            profileStore.setStreak(1)
        } else if (lastCompletionDate == todayStr) {
            // Already completed a task today
        } else {
            try {
                val lastDate = sdf.parse(lastCompletionDate)
                val todayDate = sdf.parse(todayStr)
                val diffMs = todayDate.time - lastDate.time
                val diffDays = diffMs / (1000 * 60 * 60 * 24)
                if (diffDays == 1L) {
                    profileStore.setStreak(currentStreak + 1)
                } else if (diffDays > 1L) {
                    profileStore.setStreak(1)
                }
            } catch (e: Exception) {
                profileStore.setStreak(1)
            }
        }
        profileStore.setLastCompletionDate(todayStr)
        
        _xp.value = nextXp
        _level.value = nextLevel
        _streak.value = profileStore.getStreak()
    }

    // Subtask CRUD wrappers
    fun getSubTasksForTask(taskId: Long): kotlinx.coroutines.flow.Flow<List<SubTaskEntity>> {
        return repository.getSubTasksForTask(taskId)
    }

    fun addSubTask(title: String, taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSubTask(SubTaskEntity(taskId = taskId, title = title, isCompleted = false))
        }
    }

    fun toggleSubTaskCompletion(subTask: SubTaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSubTask(subTask.copy(isCompleted = !subTask.isCompleted))
        }
    }

    fun deleteSubTask(subTaskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSubTaskById(subTaskId)
        }
    }

    // AI Chat Assistant implementation
    fun sendChatMessage(context: Context, text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage("User", text)
        _chatMessages.value = _chatMessages.value + userMsg
        
        viewModelScope.launch(Dispatchers.IO) {
            _isChatLoading.value = true
            try {
                val tasks = repository.getAllTasksOnce()
                val pendingStr = tasks.filter { it.status == "pending" }.joinToString("\n") { 
                    "- [#${it.id}] ${it.title} (Priority: ${it.priority}, Category: ${it.category}, Deadline: ${it.deadlineDate} ${it.deadlineTime})"
                }
                
                val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                
                val systemPrompt = """
                    You are Taskline AI, a personal productivity assistant. You help the user manage their tasks. Today's date is $todayDate.
                    
                    The user's current pending tasks are:
                    $pendingStr
                    
                    Respond conversationally to the user's message. If the user wants to add, complete, or delete a task, you MUST append a single JSON command line on a new line at the very end of your response. Keep the JSON command completely valid and on one single line. Do not wrap the JSON block in markdown backticks.
                    
                    Allowed commands (only one command per response):
                    1. To ADD a task:
                    JSON_COMMAND: {"command": "add", "title": "Buy groceries", "date": "YYYY-MM-DD", "time": "HH:MM", "priority": "low/medium/high", "category": "General/Work/Personal/Shopping/Health"}
                    (Important: If no date/time/priority/category is specified, guess relative date or fallback to tomorrow, "12:00", "medium", and "General").
                    
                    2. To COMPLETE a task:
                    JSON_COMMAND: {"command": "complete", "taskId": 123}
                    (Important: match the correct taskId from the list above based on the title).
                    
                    3. To DELETE a task:
                    JSON_COMMAND: {"command": "delete", "taskId": 123}
                    
                    Keep your conversational reply concise, helpful, and under 3 sentences.
                """.trimIndent()
                
                val response = callGeminiApi(systemPrompt, text)
                val (conversationalReply, jsonCommand) = extractJsonCommand(response)
                
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + ChatMessage("Taskline AI", conversationalReply)
                }
                
                if (jsonCommand != null) {
                    executeAiCommand(context, jsonCommand)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + ChatMessage("Taskline AI", "Sorry, I encountered an error: ${e.localizedMessage ?: "Unknown error"}")
                }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    private fun callGeminiApi(systemPrompt: String, userMessage: String): String {
        val key = geminiApiKey.value
        if (key.isBlank()) return "Please configure a Gemini API Key in Settings to chat with me!"
        
        try {
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val requestBodyJson = org.json.JSONObject().apply {
                val contents = org.json.JSONArray().apply {
                    val contentObj = org.json.JSONObject().apply {
                        val parts = org.json.JSONArray().apply {
                            val partObj = org.json.JSONObject().apply {
                                put("text", "$systemPrompt\n\nUser: $userMessage")
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            java.io.OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBodyJson.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(response)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return parts.getJSONObject(0).optString("text", "")
                        }
                    }
                }
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                return "Error calling Gemini API: ${conn.responseCode} - $errorStream"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Failed to connect to Gemini API: ${e.localizedMessage}"
        }
        return "No response from Gemini API"
    }

    private fun extractJsonCommand(aiResponse: String): Pair<String, org.json.JSONObject?> {
        val lines = aiResponse.split("\n")
        var jsonLine: String? = null
        val conversationalLines = mutableListOf<String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("JSON_COMMAND:") || trimmed.startsWith("{\"command\"")) {
                jsonLine = if (trimmed.startsWith("JSON_COMMAND:")) {
                    trimmed.substringAfter("JSON_COMMAND:").trim()
                } else {
                    trimmed
                }
            } else {
                conversationalLines.add(line)
            }
        }
        
        val conversationalText = conversationalLines.joinToString("\n").trim()
        val jsonObject = if (!jsonLine.isNullOrBlank()) {
            try {
                org.json.JSONObject(jsonLine)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        
        return Pair(conversationalText.ifBlank { "I have processed your request." }, jsonObject)
    }

    private fun executeAiCommand(context: Context, jsonCommand: org.json.JSONObject) {
        val command = jsonCommand.optString("command")
        if (command == "add") {
            val title = jsonCommand.optString("title", "New Task")
            val date = jsonCommand.optString("date", "")
            val time = jsonCommand.optString("time", "23:59")
            val priority = jsonCommand.optString("priority", "medium")
            val category = jsonCommand.optString("category", "General")
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val finalDate = date.ifBlank { todayDate }
            val deadlineTimestamp = try {
                sdf.parse("$finalDate $time")?.time ?: (System.currentTimeMillis() + 86400000)
            } catch (e: Exception) {
                System.currentTimeMillis() + 86400000
            }
            
            val task = TaskEntity(
                title = title,
                originalMessage = "Created via AI Assistant Chat",
                sender = "AI Chat Assistant",
                deadlineDate = finalDate,
                deadlineTime = time,
                deadlineTimestamp = deadlineTimestamp,
                priority = priority,
                category = category,
                reminderMinutesBefore = 60,
                sourceApp = "AI Assistant"
            )
            upsertTask(context, task)
        } else if (command == "complete") {
            val taskId = jsonCommand.optLong("taskId", -1)
            if (taskId != -1L) {
                viewModelScope.launch(Dispatchers.IO) {
                    val task = repository.getTaskById(taskId)
                    if (task != null) {
                        completeTask(context, task)
                    }
                }
            }
        } else if (command == "delete") {
            val taskId = jsonCommand.optLong("taskId", -1)
            if (taskId != -1L) {
                deleteTask(context, taskId)
            }
        }
    }

    fun upsertTask(context: Context, task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertTask(task)
            val updatedTask = if (task.id == 0L) task.copy(id = id) else task
            ReminderManager.scheduleReminder(context, updatedTask)
        }
    }

    fun deleteTask(context: Context, taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderManager.cancelReminder(context, taskId)
            repository.deleteTaskById(taskId)
        }
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
