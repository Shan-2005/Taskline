package com.example.chattaskai.service

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Project-Specific "Local Model" for Task Extraction.
 * Handles 80% of common task formats offline with zero latency.
 */
class LocalTaskParser {

    fun parse(text: String, strict: Boolean = true): ParsedTask? {
        val lowerText = text.lowercase()
        
        // 1. Matrix Check: Does it have an action + timeframe, or action + request?
        val actionKeywords = listOf("remind", "buy", "call", "send", "submit", "fix", "check", "finish", "complete", "create", "write", "prepare", "report", "document", "todo", "appointment", "meeting", "deadline", "urgent", "review", "update", "cancel", "come", "go", "visit", "attend", "join", "meet", "do", "start", "wrap", "get", "bring", "give", "ask", "tell", "show", "pay", "order", "deliver", "assignment", "submission")
        val shorthandActionPhrases = listOf("have work", "work at", "work on", "work today", "work tomorrow", "have a meeting", "have meeting")
        val requestKeywords = listOf("please", "can you", "could you", "need to", "have to", "must", "make sure", "don't forget", "task:", "todo:", "remind me", "i need", "i want", "kindly", "ensure", "priority", "required")
        val temporalMarkers = listOf("today", "tomorrow", "tonight", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "morning", "evening", "afternoon", "pm", "am", "noon", "lunch", "dinner", "eod", "hour", "hr", "min", "month", "week", "daily", "weekly", "monthly", "anytime")
        
        val hasAction = actionKeywords.any { matchesKeyword(lowerText, it) } || shorthandActionPhrases.any { lowerText.contains(it) }
        val hasRequest = requestKeywords.any { lowerText.contains(it) }
        val hasTemporalMarker = temporalMarkers.any { matchesKeyword(lowerText, it) } || text.contains("\\d{1,2}:\\d{2}|\\d{1,2}\\s*(am|pm|hrs)".toRegex(RegexOption.IGNORE_CASE)) || lowerText.contains(" at ") || lowerText.contains(" by ") || text.contains("\\d{1,2}/\\d{1,2}".toRegex()) || text.contains("\\d{4}-\\d{2}-\\d{2}".toRegex())

        // Reject plain conversational text
        if (strict) {
            // Must be explicitly a request action, or a scheduled action
            if (!(hasAction && hasRequest) && !(hasAction && hasTemporalMarker)) return null
            if (text.length < 10) return null
        } else {
            // Lenient: Just need an action
            if (!hasAction) return null
        }

        // 2. Extract Date
        val date = extractDate(lowerText)
        
        // 3. Extract Time
        val time = extractTime(lowerText)

        // 4. Extract Title 
        var titleFromLines = text.lines().firstOrNull { it.trim().isNotEmpty() && !it.contains(":") } ?: text.split(":").last().trim()
        
        // Specific check for "Role: X" or "hiring for the role of X"
        val roleRegex = Regex("(?i)role of ([^.]+)|role: ([^.]+)")
        val roleMatch = roleRegex.find(text)
        var cleanTitle = if (roleMatch != null) {
            "Apply: " + (roleMatch.groupValues[1].ifEmpty { roleMatch.groupValues[2] }).trim()
        } else {
            titleFromLines
        }

        val stopWords = listOf("remind me to", "remind me", "please", "can you", "i need to", "tomorrow", "today", "tonight", "at", "by", "on", "deadline", "deadline:")
        stopWords.forEach { word ->
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b$word\\b"), "").trim()
        }
        
        // Final cleanup of punctuation and emojis at start/end
        cleanTitle = cleanTitle.trim { !it.isLetterOrDigit() }
        
        if (cleanTitle.isEmpty()) cleanTitle = "Task from WhatsApp"
        if (cleanTitle.length > 50) cleanTitle = cleanTitle.take(47) + "..."

        // 5. Determine Priority
        val priority = when {
            lowerText.contains("urgent") || lowerText.contains("asap") || lowerText.contains("immediately") -> "high"
            lowerText.contains("deadline") || lowerText.contains("important") -> "medium"
            else -> "low"
        }

        val isTask = if (strict) {
            (hasAction && hasRequest) || (hasAction && hasTemporalMarker)
        } else {
            hasAction
        }

        return ParsedTask(
            task = cleanTitle.replaceFirstChar { it.uppercase() },
            date = date,
            time = time,
            priority = priority,
            category = "General", // Local model defaults to General
            is_task = isTask
        )
    }

    private fun matchesKeyword(text: String, keyword: String): Boolean {
        return if (keyword.contains(" ")) {
            text.contains(keyword)
        } else {
            Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }
    }

    private fun extractDate(text: String): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return when {
            text.contains("tomorrow") -> {
                now.add(Calendar.DAY_OF_YEAR, 1)
                sdf.format(now.time)
            }
            text.contains("day after tomorrow") -> {
                now.add(Calendar.DAY_OF_YEAR, 2)
                sdf.format(now.time)
            }
            text.contains("tonight") || text.contains("today") -> {
                sdf.format(now.time)
            }
            // Absolute Date Pattern: 3rd April 2026 or 22 April
            Regex("""(\d{1,2}(?:st|nd|rd|th)?)\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\s+,?\s*(\d{4}))?""").find(text) != null -> {
                val match = Regex("""(\d{1,2}(?:st|nd|rd|th)?)\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\s+,?\s*(\d{4}))?""").find(text)!!
                val dayStr = match.groupValues[1].replace(Regex("(st|nd|rd|th)"), "")
                val monthStr = match.groupValues[2].take(3).lowercase()
                val yearStr = match.groupValues[3]
                
                val monthMap = mapOf("jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3, "may" to 4, "jun" to 5, "jul" to 6, "aug" to 7, "sep" to 8, "oct" to 9, "nov" to 10, "dec" to 11)
                val targetMonth = monthMap[monthStr] ?: 0
                val targetDay = dayStr.toInt()
                val targetYear = if (yearStr.isNotEmpty()) yearStr.toInt() else now.get(Calendar.YEAR)
                
                now.set(targetYear, targetMonth, targetDay)
                sdf.format(now.time)
            }
            text.contains("monday") -> getNextDayOfWeek(Calendar.MONDAY)
            text.contains("tuesday") -> getNextDayOfWeek(Calendar.TUESDAY)
            text.contains("wednesday") -> getNextDayOfWeek(Calendar.WEDNESDAY)
            text.contains("thursday") -> getNextDayOfWeek(Calendar.THURSDAY)
            text.contains("friday") -> getNextDayOfWeek(Calendar.FRIDAY)
            text.contains("saturday") -> getNextDayOfWeek(Calendar.SATURDAY)
            text.contains("sunday") -> getNextDayOfWeek(Calendar.SUNDAY)
            else -> sdf.format(now.time)
        }
    }

    private fun getNextDayOfWeek(dayOfWeek: Int): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        var daysUntil = dayOfWeek - currentDay
        if (daysUntil <= 0) daysUntil += 7
        now.add(Calendar.DAY_OF_YEAR, daysUntil)
        return sdf.format(now.time)
    }

    private fun extractTime(text: String): String {
        // 0. Preliminary cleanup: remove ordinal indicators like 1st, 2nd, 3rd, 4th to avoid confusion with time
        val sanitizedText = text.replace(Regex("\\b(\\d+)(st|nd|rd|th)\\b"), " ")

        // 1. Handle "in X hours/minutes"
        val offsetPattern = Pattern.compile("in (\\d+) (hour|minute|hr|min)")
        val offsetMatcher = offsetPattern.matcher(sanitizedText)
        if (offsetMatcher.find()) {
            val amount = offsetMatcher.group(1)?.toInt() ?: 0
            val unit = offsetMatcher.group(2)
            val now = Calendar.getInstance()
            if (unit?.startsWith("h") == true) now.add(Calendar.HOUR_OF_DAY, amount)
            else now.add(Calendar.MINUTE, amount)
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        }

        // 2. High Priority: Explicitly look for HH:MM (possibly with 'hrs' or 'am/pm')
        // Regex: Matches 00:00 to 23:59, optional : or ., optional hrs, optional am/pm
        val preciseTimePattern = Pattern.compile("([012]?\\d)[:.](\\d{2})\\s*(hrs|am|pm)?")
        val preciseMatcher = preciseTimePattern.matcher(sanitizedText)
        if (preciseMatcher.find()) {
            var hour = preciseMatcher.group(1)?.toInt() ?: 9
            val minute = preciseMatcher.group(2)?.toInt() ?: 0
            val suffix = preciseMatcher.group(3)?.lowercase() ?: ""

            if (suffix == "pm" && hour < 12) hour += 12
            if (suffix == "am" && hour == 12) hour = 0
            
            return String.format("%02d:%02d", hour, minute)
        }

        // 3. Fallback: Standard time patterns: 10am, 10 PM
        val fallbackPattern = Pattern.compile("(\\d{1,2})\\s*(am|pm)")
        val fallbackMatcher = fallbackPattern.matcher(sanitizedText)
        if (fallbackMatcher.find()) {
            var hour = fallbackMatcher.group(1)?.toInt() ?: 9
            val ampm = fallbackMatcher.group(2)?.lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return String.format("%02d:00", hour)
        }

        return "23:59"
    }
}
