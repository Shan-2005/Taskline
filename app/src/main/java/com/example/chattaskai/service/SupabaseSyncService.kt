package com.example.chattaskai.service

import com.example.chattaskai.BuildConfig
import com.example.chattaskai.data.database.TaskEntity
import com.example.chattaskai.data.profile.ProfileStore
import com.example.chattaskai.data.profile.UserProfile
import com.example.chattaskai.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class SupabaseSyncService(
    private val repository: TaskRepository,
    private val profileStore: ProfileStore
) {
    private val httpClient = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()

    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext SyncResult(0, 0, "Supabase is not configured in gradle.properties")
        }

        val profile = profileStore.loadProfile()
        val ownerKey = ownerKey(profile)

        upsertProfile(ownerKey, profile)

        val localTasks = repository.getAllTasksOnce()
        pushTasks(ownerKey, localTasks)

        val remoteTasks = fetchTasks(ownerKey)
        var pulled = 0
        for (task in remoteTasks) {
            repository.upsertNotificationTask(task)
            pulled += 1
        }

        SyncResult(
            pushed = localTasks.size,
            pulled = pulled,
            message = "Supabase sync complete"
        )
    }

    private fun ownerKey(profile: UserProfile): String {
        val preferred = profile.email.ifBlank { profile.displayName }
            .ifBlank { "taskline_user" }
            .trim()
            .lowercase()
        return preferred.replace(" ", "_")
    }

    private fun upsertProfile(ownerKey: String, profile: UserProfile) {
        val payload = JSONArray().put(
            JSONObject().apply {
                put("owner_key", ownerKey)
                put("display_name", profile.displayName)
                put("email", profile.email)
                put("organization", profile.organization)
            }
        )

        val endpoint = "${BuildConfig.SUPABASE_URL}/rest/v1/${BuildConfig.SUPABASE_PROFILES_TABLE}?on_conflict=owner_key"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Profile sync failed: ${response.code} ${response.message}")
            }
        }
    }

    private fun pushTasks(ownerKey: String, tasks: List<TaskEntity>) {
        if (tasks.isEmpty()) return

        val payload = JSONArray()
        tasks.forEach { task ->
            payload.put(
                JSONObject().apply {
                    put("owner_key", ownerKey)
                    put("title", task.title)
                    put("original_message", task.originalMessage)
                    put("sender", task.sender)
                    put("deadline_date", task.deadlineDate)
                    put("deadline_time", task.deadlineTime)
                    put("deadline_timestamp", task.deadlineTimestamp)
                    put("priority", task.priority)
                    put("category", task.category)
                    put("reminder_minutes_before", task.reminderMinutesBefore)
                    put("source_app", task.sourceApp)
                    put("status", task.status)
                    put("created_at", task.createdAt)
                }
            )
        }

        val endpoint = "${BuildConfig.SUPABASE_URL}/rest/v1/${BuildConfig.SUPABASE_TASKS_TABLE}?on_conflict=owner_key,source_app,sender,original_message"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Task push failed: ${response.code} ${response.message}")
            }
        }
    }

    private fun fetchTasks(ownerKey: String): List<TaskEntity> {
        val encodedOwner = URLEncoder.encode("eq.$ownerKey", "UTF-8")
        val select = URLEncoder.encode(
            "title,original_message,sender,deadline_date,deadline_time,deadline_timestamp,priority,category,reminder_minutes_before,source_app,status,created_at",
            "UTF-8"
        )

        val endpoint = "${BuildConfig.SUPABASE_URL}/rest/v1/${BuildConfig.SUPABASE_TASKS_TABLE}?owner_key=$encodedOwner&select=$select"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Task fetch failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string().orEmpty()
            if (responseBody.isBlank()) return emptyList()

            val array = JSONArray(responseBody)
            val tasks = mutableListOf<TaskEntity>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                tasks += TaskEntity(
                    title = item.optString("title", "Task"),
                    originalMessage = item.optString("original_message", ""),
                    sender = item.optString("sender", "Unknown Sender"),
                    deadlineDate = item.optString("deadline_date", ""),
                    deadlineTime = item.optString("deadline_time", ""),
                    deadlineTimestamp = item.optLong("deadline_timestamp", System.currentTimeMillis()),
                    priority = item.optString("priority", "low"),
                    category = item.optString("category", "General"),
                    reminderMinutesBefore = item.optInt("reminder_minutes_before", 60),
                    sourceApp = item.optString("source_app", "Supabase"),
                    status = item.optString("status", "pending"),
                    createdAt = item.optLong("created_at", System.currentTimeMillis())
                )
            }
            return tasks
        }
    }
}

data class SyncResult(
    val pushed: Int,
    val pulled: Int,
    val message: String
)
