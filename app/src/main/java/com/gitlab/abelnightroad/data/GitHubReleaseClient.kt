package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The latest GitHub release that ships an APK. [version] keeps the raw tag (e.g. `"v1.6.0"`). */
data class LatestRelease(val version: String, val notes: String, val apkUrl: String)

/**
 * GitHub Releases update source for the sideloaded APK (no Play Store).
 * Pure JVM: JSON parsing and the semver comparison are separable from the
 * [HttpURLConnection] fetch so they are unit-testable against fixtures.
 */
object GitHubReleaseClient {

    private const val RELEASES_URL =
        "https://api.github.com/repos/AbelNightroad/card_locator/releases/latest"
    private const val USER_AGENT = "MtGCardTracker/1.0"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ReleaseAsset(
        val name: String = "",
        @SerialName("browser_download_url") val browserDownloadUrl: String = ""
    )

    @Serializable
    private data class ReleaseResponse(
        @SerialName("tag_name") val tagName: String = "",
        val body: String = "",
        val assets: List<ReleaseAsset> = emptyList(),
        val message: String? = null
    )

    /**
     * Maps a non-200 GitHub response body to an explicit error message —
     * unauthenticated API calls are rate-limited to 60/hour.
     */
    fun apiErrorMessage(code: Int, body: String): String {
        val message = runCatching {
            json.decodeFromString<Map<String, String>>(body)["message"]
        }.getOrNull() ?: return "GitHub API returned HTTP $code"
        return if (message.contains("rate limit", ignoreCase = true)) {
            "GitHub rate limit — try again later"
        } else {
            message
        }
    }

    /** Parses the release JSON; throws an explicit [IOException] on unusable payloads. */
    fun parseRelease(jsonText: String): LatestRelease {
        val resp = try {
            json.decodeFromString<ReleaseResponse>(jsonText)
        } catch (e: Exception) {
            throw IOException("Malformed GitHub release response")
        }
        resp.message?.let { throw IOException(apiErrorMessage(403, jsonText)) }
        if (resp.tagName.isBlank()) throw IOException("GitHub release has no tag")
        val apk = resp.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
        } ?: throw IOException("Latest release has no APK asset")
        return LatestRelease(version = resp.tagName, notes = resp.body, apkUrl = apk.browserDownloadUrl)
    }

    /** Fetches the latest release; errors are explicit messages, never silent. */
    suspend fun fetchLatest(): LatestRelease = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = runCatching {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }.getOrDefault("")
                throw IOException(apiErrorMessage(code, errorBody))
            }
            parseRelease(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(e.message ?: "Update check failed")
        } finally {
            conn.disconnect()
        }
    }

    /**
     * True when [tag] is a strictly newer semver than [current]. Segments are
     * compared numerically after stripping an optional `v` prefix, so
     * `1.10.0` > `1.9.3`. Non-numeric suffixes compare as their numeric part.
     */
    fun isNewerThan(current: String, tag: String): Boolean {
        val currentParts = current.removePrefix("v").removePrefix("V").split(".")
        val tagParts = tag.removePrefix("v").removePrefix("V").split(".")
        for (i in 0 until maxOf(currentParts.size, tagParts.size)) {
            val a = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val b = tagParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (a != b) return b > a
        }
        return false
    }
}
