package com.example.chattaskai.service

import com.example.chattaskai.data.profile.TrackingSnapshot

object NotificationSourceMatcher {

    fun shouldTrack(
        packageName: String,
        title: String,
        body: String,
        snapshot: TrackingSnapshot
    ): Boolean {
        val sourceKey = when {
            packageName.contains("whatsapp") -> "whatsapp"
            packageName.contains("gm") -> "gmail"
            packageName.contains("outlook") -> "outlook"
            else -> return false
        }

        if (!snapshot.allowedApps.contains(sourceKey)) return false

        val haystack = listOf(title, body).joinToString("\n").lowercase()
        val sourceFilters = when (sourceKey) {
            "whatsapp" -> if (snapshot.whatsappFilterEnabled) snapshot.whatsappFilters else emptyList()
            "gmail" -> if (snapshot.gmailFilterEnabled) snapshot.gmailFilters else emptyList()
            else -> if (snapshot.gmailFilterEnabled) snapshot.gmailFilters else emptyList()
        }

        val sourceAllowed = sourceFilters.isEmpty() || sourceFilters.any { haystack.contains(it.lowercase()) }
        if (!sourceAllowed) return false

        val keywordAllowed = snapshot.messageKeywords.isEmpty() || snapshot.messageKeywords.any { haystack.contains(it.lowercase()) }
        return keywordAllowed
    }
}
