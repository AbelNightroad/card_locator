package com.gitlab.abelnightroad.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the update APK into `cacheDir/updates/latest.apk` and hands it
 * to the system installer through the app's FileProvider.
 */
object AppUpdater {

    private const val USER_AGENT = "MtGCardTracker/1.0"
    private const val APK_MIME = "application/vnd.android.package-archive"

    fun apkFile(context: Context): File =
        File(File(context.cacheDir, "updates").apply { mkdirs() }, "latest.apk")

    /**
     * Downloads [url] to [dest], deleting any stale file first and reporting
     * progress as (bytesDone, bytesTotal); [bytesTotal] is -1 when the server
     * omits Content-Length. Throws [IOException] with an explicit message.
     */
    suspend fun download(
        url: String,
        dest: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        if (dest.exists()) dest.delete()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 0
            setRequestProperty("User-Agent", USER_AGENT)
            instanceFollowRedirects = true
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("Download failed — HTTP $code")
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            dest.delete()
            throw e
        } catch (e: Exception) {
            dest.delete()
            throw if (e is IOException) e else IOException(e.message ?: "Download failed")
        } finally {
            conn.disconnect()
        }
    }

    /**
     * ACTION_VIEW install intent for [file] via the app's FileProvider, or
     * null when no activity can handle it (caller surfaces the failure).
     */
    fun installIntent(context: Context, file: File): Intent? {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }
}
