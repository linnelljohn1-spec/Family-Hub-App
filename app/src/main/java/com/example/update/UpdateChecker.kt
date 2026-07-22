package com.example.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Checks GitHub's "latest release" API for a newer build than the one currently running. */
object UpdateChecker {
    private const val RELEASES_API_URL =
        "https://api.github.com/repos/linnelljohn1-spec/Family-Hub-App/releases/latest"

    /** Returns update info if a newer release is available, or null on any failure (no network, no release, bad data). */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(RELEASES_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                val json = connection.inputStream.use { JSONObject(it.bufferedReader().readText()) }
                val tagName = json.optString("tag_name", "")
                val versionCode = tagName.removePrefix("v").toIntOrNull() ?: return@withContext null

                val assets = json.optJSONArray("assets") ?: return@withContext null
                val apkAsset = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") } ?: return@withContext null

                UpdateInfo(
                    versionCode = versionCode,
                    versionName = json.optString("name", tagName),
                    downloadUrl = apkAsset.getString("browser_download_url"),
                    releaseUrl = json.optString("html_url", "")
                )
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }
}
