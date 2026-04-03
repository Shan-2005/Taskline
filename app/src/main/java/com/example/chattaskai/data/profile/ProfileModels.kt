package com.example.chattaskai.data.profile

data class UserProfile(
    val displayName: String = "Taskline User",
    val email: String = "",
    val organization: String = "",
    val onboardingComplete: Boolean = false
)

data class TrackingRules(
    val trackWhatsApp: Boolean = true,
    val trackGmail: Boolean = true,
    val trackOutlook: Boolean = false,
    val gmailFilters: String = "",
    val whatsappFilters: String = "",
    val messageKeywords: String = ""
)

data class TrackingSnapshot(
    val allowedApps: Set<String>,
    val gmailFilters: List<String>,
    val whatsappFilters: List<String>,
    val messageKeywords: List<String>
)
