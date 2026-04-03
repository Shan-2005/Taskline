package com.example.chattaskai.service

import com.example.chattaskai.data.profile.TrackingSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSourceMatcherTest {

    @Test
    fun whatsappRules_matchAllowedGroupAndKeyword() {
        val snapshot = TrackingSnapshot(
            allowedApps = setOf("whatsapp"),
            gmailFilters = emptyList(),
            whatsappFilters = listOf("Project Team"),
            messageKeywords = listOf("deadline")
        )

        val shouldTrack = NotificationSourceMatcher.shouldTrack(
            packageName = "com.whatsapp",
            title = "Project Team",
            body = "Please review the deadline today",
            snapshot = snapshot
        )

        assertTrue(shouldTrack)
    }

    @Test
    fun gmailRules_rejectNonMatchingSender() {
        val snapshot = TrackingSnapshot(
            allowedApps = setOf("gmail"),
            gmailFilters = listOf("boss@company.com"),
            whatsappFilters = emptyList(),
            messageKeywords = listOf("submit")
        )

        val shouldTrack = NotificationSourceMatcher.shouldTrack(
            packageName = "com.google.android.gm",
            title = "hr@company.com",
            body = "Please submit timesheet",
            snapshot = snapshot
        )

        assertFalse(shouldTrack)
    }
}
