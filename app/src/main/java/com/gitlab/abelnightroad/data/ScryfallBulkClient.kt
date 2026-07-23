package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Scryfall "Default Cards" bulk-data locator. */
data class BulkDataMeta(val downloadUri: String, val updatedAt: String)

/**
 * Talks to the Scryfall Bulk Data API. Uses only the JDK
 * ([java.net.HttpURLConnection]) so no networking dependency is added.
 */
object ScryfallBulkClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val META_URL = "https://api.scryfall.com/bulk-data/default_cards"
    private const val USER_AGENT = "MtGCardTracker/1.0"

    @Serializable
    private data class BulkDataResponse(
        @SerialName("jsonl_download_uri") val jsonlDownloadUri: String? = null,
        @SerialName("download_uri") val downloadUri: String = "",
        @SerialName("updated_at") val updatedAt: String = ""
    )

    /** Parses the bulk-data metadata JSON. Pure so it can be unit-tested. */
    fun parseMeta(jsonText: String): BulkDataMeta {
        val resp = json.decodeFromString<BulkDataResponse>(jsonText)
        return BulkDataMeta(
            downloadUri = resp.jsonlDownloadUri?.takeIf { it.isNotBlank() } ?: resp.downloadUri,
            updatedAt = resp.updatedAt
        )
    }

    suspend fun getDefaultCardsMeta(): BulkDataMeta = withContext(Dispatchers.IO) {
        val conn = (URL(META_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "no error body"
                } catch (_: Exception) { "no error body" }
                throw IOException("Scryfall API returned $code: $errorBody")
            }
            conn.inputStream.bufferedReader().use { parseMeta(it.readText()) }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Downloads [uri] to [dest], reporting progress as (bytesDone, bytesTotal).
     * [bytesTotal] is -1 when the server omits Content-Length.
     */
    suspend fun download(
        uri: String,
        dest: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val conn = (URL(uri).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 0
            setRequestProperty("User-Agent", USER_AGENT)
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "no error body"
                } catch (_: Exception) { "no error body" }
                throw IOException("Scryfall download returned $code: $errorBody")
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
