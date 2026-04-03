package com.example.chattaskai.service

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ApkUpdateInfo(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object GitHubApkUpdateChecker {

    fun findAvailableUpdate(
        context: Context,
        repoOwner: String,
        repoName: String,
        assetPrefix: String
    ): ApkUpdateInfo? {
        if (repoOwner.isBlank() || repoName.isBlank()) return null

        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(json)
            val tagName = release.optString("tag_name", "")
            val body = release.optString("body", "")
            val assets = release.optJSONArray("assets") ?: return null

            var selectedDownloadUrl = ""
            var selectedVersionCode = -1L

            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name", "")
                val browserDownloadUrl = asset.optString("browser_download_url", "")

                if (!name.endsWith(".apk", ignoreCase = true)) continue
                if (!name.startsWith(assetPrefix)) continue

                val versionCode = parseVersionCodeFromAssetName(name)
                if (versionCode > selectedVersionCode && browserDownloadUrl.isNotBlank()) {
                    selectedVersionCode = versionCode
                    selectedDownloadUrl = browserDownloadUrl
                }
            }

            if (selectedVersionCode <= 0L || selectedDownloadUrl.isBlank()) return null

            val currentVersionCode = getCurrentVersionCode(context)
            if (selectedVersionCode <= currentVersionCode) return null

            return ApkUpdateInfo(
                latestVersionCode = selectedVersionCode,
                latestVersionName = tagName.ifBlank { "v$selectedVersionCode" },
                downloadUrl = selectedDownloadUrl,
                releaseNotes = body
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVersionCodeFromAssetName(assetName: String): Long {
        val match = Regex("vc(\\d+)").find(assetName) ?: return -1L
        return match.groupValues.getOrNull(1)?.toLongOrNull() ?: -1L
    }

    private fun getCurrentVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
