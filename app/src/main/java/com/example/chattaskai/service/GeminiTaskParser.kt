package com.example.chattaskai.service

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GeminiTaskParser(private val apiKey: String) {

    private fun logDebug(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (e: RuntimeException) {
            println("[$tag] $msg")
        }
    }

    private fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(tag, msg, tr)
        } catch (e: RuntimeException) {
            println("ERROR: [$tag] $msg")
            tr?.printStackTrace()
        }
    }

    fun parse(text: String, strict: Boolean = true): ParsedTask? {
        if (apiKey.isBlank()) {
            logDebug("GeminiTaskParser", "API key is blank, skipping Gemini parsing.")
            return null
        }
        
        try {
            val now = Calendar.getInstance()
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(now.time)

            // Using gemini-1.5-flash for general task parsing
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            // Construct Gemini request JSON
            val requestBodyJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", "You are an expert task extractor. Today's date is $todayDate ($dayOfWeek), and the current time is $currentTime.\n\n" +
                                        "Analyze this message: \"\"\"$text\"\"\".\n" +
                                        "Extract the task details. If the message is not an actionable task, set is_task to false.\n" +
                                        "If it is a task, extract the title (max 50 chars), deadline date (use $todayDate if none specified), deadline time (use 23:59 if none specified), priority (low, medium, or high), and category (e.g. Work, Personal, Urgent, Meeting, Shopping, General).")
                            }
                            put(partObj)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                // Enforce JSON output schema
                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    val responseSchema = JSONObject().apply {
                        put("type", "OBJECT")
                        val properties = JSONObject().apply {
                            put("task", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "A concise task title summarizing the action. Clean, capitalized first letter.")
                            })
                            put("date", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Deadline date in YYYY-MM-DD format. Resolve relative expressions relative to $todayDate.")
                            })
                            put("time", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Deadline time in HH:MM format. Resolve relative expressions relative to $currentTime.")
                            })
                            put("priority", JSONObject().apply {
                                put("type", "STRING")
                                put("enum", JSONArray(listOf("low", "medium", "high")))
                            })
                            put("category", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "The task category (e.g., Work, Personal, Meeting, General).")
                            })
                            put("is_task", JSONObject().apply {
                                put("type", "BOOLEAN")
                                put("description", "Set to true if there is an actual actionable task. Set to false if it's general conversation or no action is required.")
                            })
                        }
                        put("properties", properties)
                        put("required", JSONArray(listOf("task", "date", "time", "priority", "category", "is_task")))
                    }
                    put("responseSchema", responseSchema)
                }
                put("generationConfig", generationConfig)
            }

            logDebug("GeminiTaskParser", "Sending request to Gemini: ${requestBodyJson.toString()}")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBodyJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                logDebug("GeminiTaskParser", "Received response from Gemini: $response")
                
                val root = JSONObject(response)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val textPart = parts.getJSONObject(0).optString("text", "")
                            if (textPart.isNotBlank()) {
                                val jsonOutput = JSONObject(textPart)
                                
                                val isTask = jsonOutput.optBoolean("is_task", false)
                                val task = jsonOutput.optString("task", "Task from WhatsApp")
                                val date = jsonOutput.optString("date", todayDate)
                                val time = jsonOutput.optString("time", "23:59")
                                val priority = jsonOutput.optString("priority", "low")
                                val category = jsonOutput.optString("category", "General")

                                if (strict && !isTask) {
                                    logDebug("GeminiTaskParser", "Strict filter reject: is_task is false.")
                                    return null
                                }

                                return ParsedTask(
                                    task = task,
                                    date = date,
                                    time = time,
                                    priority = priority,
                                    category = category,
                                    is_task = isTask
                                )
                            }
                        }
                    }
                }
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                logError("GeminiTaskParser", "API Error: Response Code = $responseCode, Error = $errorStream")
            }
        } catch (e: Exception) {
            logError("GeminiTaskParser", "Exception during Gemini parsing: ${e.message}", e)
        }
        return null
    }
}
