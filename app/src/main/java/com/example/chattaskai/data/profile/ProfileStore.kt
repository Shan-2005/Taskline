package com.example.chattaskai.data.profile

import android.content.Context
import org.json.JSONObject

class ProfileStore(context: Context) {
    private val profilePrefs = context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    private val trackingPrefs = context.getSharedPreferences(TRACKING_PREFS, Context.MODE_PRIVATE)
    private val knownSourcesPrefs = context.getSharedPreferences(KNOWN_SOURCES_PREFS, Context.MODE_PRIVATE)

    fun loadProfile(): UserProfile {
        val json = profilePrefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return runCatching {
            val obj = JSONObject(json)
            UserProfile(
                phoneNumber = obj.optString("phoneNumber", ""),
                displayName = obj.optString("displayName", "Taskline User"),
                email = obj.optString("email", ""),
                organization = obj.optString("organization", ""),
                registrationComplete = profilePrefs.getBoolean(KEY_REGISTRATION_COMPLETE, false),
                onboardingComplete = profilePrefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
            )
        }.getOrElse { UserProfile(registrationComplete = profilePrefs.getBoolean(KEY_REGISTRATION_COMPLETE, false), onboardingComplete = profilePrefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)) }
    }

    fun saveProfile(profile: UserProfile) {
        val json = JSONObject().apply {
            put("phoneNumber", profile.phoneNumber)
            put("displayName", profile.displayName)
            put("email", profile.email)
            put("organization", profile.organization)
        }
        profilePrefs.edit()
            .putString(KEY_PROFILE, json.toString())
            .putBoolean(KEY_REGISTRATION_COMPLETE, profile.registrationComplete)
            .putBoolean(KEY_ONBOARDING_COMPLETE, profile.onboardingComplete)
            .apply()
    }

    fun loadTrackingRules(): TrackingRules {
        val json = trackingPrefs.getString(KEY_TRACKING, null) ?: return TrackingRules()
        return runCatching {
            val obj = JSONObject(json)
            TrackingRules(
                trackWhatsApp = obj.optBoolean("trackWhatsApp", true),
                trackGmail = obj.optBoolean("trackGmail", true),
                trackOutlook = obj.optBoolean("trackOutlook", false),
                gmailFilters = obj.optString("gmailFilters", ""),
                whatsappFilters = obj.optString("whatsappFilters", ""),
                messageKeywords = obj.optString("messageKeywords", "")
            )
        }.getOrElse { TrackingRules() }
    }

    fun saveTrackingRules(rules: TrackingRules) {
        val json = JSONObject().apply {
            put("trackWhatsApp", rules.trackWhatsApp)
            put("trackGmail", rules.trackGmail)
            put("trackOutlook", rules.trackOutlook)
            put("gmailFilters", rules.gmailFilters)
            put("whatsappFilters", rules.whatsappFilters)
            put("messageKeywords", rules.messageKeywords)
        }
        trackingPrefs.edit().putString(KEY_TRACKING, json.toString()).apply()
    }

    fun setOnboardingComplete(complete: Boolean) {
        profilePrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun isOnboardingComplete(): Boolean = profilePrefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun isRegistrationComplete(): Boolean = profilePrefs.getBoolean(KEY_REGISTRATION_COMPLETE, false)

    fun setRegistrationComplete(phoneNumber: String) {
        profilePrefs.edit()
            .putBoolean(KEY_REGISTRATION_COMPLETE, true)
            .apply()
        val profile = loadProfile()
        saveProfile(profile.copy(phoneNumber = phoneNumber, registrationComplete = true))
    }

    fun addKnownWhatsAppSource(source: String) {
        val normalized = source.trim()
        if (normalized.isBlank() || normalized == "Unknown Sender") return

        val current = getKnownWhatsAppSources().toMutableSet()
        current.add(normalized)
        knownSourcesPrefs.edit().putStringSet(KEY_WHATSAPP_SOURCES, current).apply()
    }

    fun getKnownWhatsAppSources(): List<String> {
        val values = knownSourcesPrefs.getStringSet(KEY_WHATSAPP_SOURCES, emptySet()).orEmpty()
        return values.map { it.trim() }
            .filter { it.isNotBlank() }
            .sortedBy { it.lowercase() }
    }

    fun snapshot(): TrackingSnapshot {
        val rules = loadTrackingRules()
        return TrackingSnapshot(
            allowedApps = buildSet {
                if (rules.trackWhatsApp) add("whatsapp")
                if (rules.trackGmail) add("gmail")
                if (rules.trackOutlook) add("outlook")
            },
            gmailFilters = splitFilters(rules.gmailFilters),
            whatsappFilters = splitFilters(rules.whatsappFilters),
            messageKeywords = splitFilters(rules.messageKeywords)
        )
    }

    private fun splitFilters(raw: String): List<String> {
        return raw.split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    companion object {
        private const val PROFILE_PREFS = "taskline_profile"
        private const val TRACKING_PREFS = "taskline_tracking"
        private const val KNOWN_SOURCES_PREFS = "taskline_known_sources"
        private const val KEY_PROFILE = "profile_json"
        private const val KEY_TRACKING = "tracking_json"
        private const val KEY_REGISTRATION_COMPLETE = "registration_complete"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_WHATSAPP_SOURCES = "known_whatsapp_sources"
    }
}
