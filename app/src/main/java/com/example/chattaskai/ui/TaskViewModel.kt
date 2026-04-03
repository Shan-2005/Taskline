package com.example.chattaskai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.chattaskai.BuildConfig
import com.example.chattaskai.data.database.TaskEntity
import com.example.chattaskai.reminder.ReminderManager
import com.example.chattaskai.service.ApkUpdateInfo
import com.example.chattaskai.service.GitHubApkUpdateChecker
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

    private val _themeHue = MutableStateFlow(0f)
    val themeHue = _themeHue.asStateFlow()

    private val _morningReminderHour = MutableStateFlow(9)
    val morningReminderHour = _morningReminderHour.asStateFlow()

    private val _snoozeMinutes = MutableStateFlow(10)
    val snoozeMinutes = _snoozeMinutes.asStateFlow()

    private val _strictFiltering = MutableStateFlow(true)
    val strictFiltering = _strictFiltering.asStateFlow()

    private val _availableApkUpdate = MutableStateFlow<ApkUpdateInfo?>(null)
    val availableApkUpdate = _availableApkUpdate.asStateFlow()

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _themeHue.value = prefs.getFloat("theme_hue", 0f)
        _morningReminderHour.value = prefs.getInt("morning_hour", 9)
        _snoozeMinutes.value = prefs.getInt("snooze_min", 10)
        _strictFiltering.value = prefs.getBoolean("strict_filter", true)
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


    fun completeTask(context: Context, task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderManager.cancelReminder(context, task.id)
            repository.updateTask(task.copy(status = "completed"))
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
