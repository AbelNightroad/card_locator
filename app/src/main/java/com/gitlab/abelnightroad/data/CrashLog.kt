package com.gitlab.abelnightroad.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists uncaught exception stack traces to `filesDir/crash.log` so crashes
 * can be reported without adb. Install once at startup; the default handler is
 * chained afterwards so the system's normal crash flow is unchanged.
 */
object CrashLog {

    private const val FILE_NAME = "crash.log"
    private const val MAX_CHARS = 64 * 1024

    @Volatile
    private var installed = false

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                write(appContext, thread, throwable)
            } catch (_: Exception) {
                // Crash reporting must never mask the original crash.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Runtime.getRuntime().halt(1)
            }
        }
    }

    fun read(context: Context): String {
        val f = file(context)
        return if (f.exists()) f.readText() else ""
    }

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val entry = buildString {
            append("=== ").append(timestamp).append(" ===\n")
            append("thread: ").append(thread.name).append('\n')
            append(throwable.stackTraceToString()).append('\n')
        }
        val f = file(context)
        val merged = entry + if (f.exists()) f.readText() else ""
        f.writeText(if (merged.length > MAX_CHARS) merged.take(MAX_CHARS) else merged)
    }
}
