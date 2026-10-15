package com.gitlab.abelnightroad.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gitlab.abelnightroad.data.AppUpdater
import com.gitlab.abelnightroad.data.LatestRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val done: Long, val total: Long) : DownloadState
    data object Ready : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * One-shot update dialog: release notes + Later / Download & Install.
 * Handles the whole flow — download progress (byte counter → percent),
 * unknown-sources permission grant and the system install prompt.
 */
@Composable
fun UpdateDialog(
    release: LatestRelease,
    currentVersion: String,
    onLater: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(release.version) { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var file by remember(release.version) { mutableStateOf<File?>(null) }
    var job by remember(release.version) { mutableStateOf<Job?>(null) }
    var permissionResult by remember(release.version) { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionResult = true
    }

    fun startInstall() {
        val apk = file ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            permissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
            return
        }
        val intent = AppUpdater.installIntent(context, apk)
        if (intent != null) {
            context.startActivity(intent)
            onDone()
        } else {
            apk.delete()
            state = DownloadState.Failed("No app can install APK files on this device")
        }
    }

    fun beginDownload() {
        state = DownloadState.Running(0, -1)
        job = scope.launch {
            try {
                val apk = AppUpdater.apkFile(context)
                AppUpdater.download(release.apkUrl, apk) { done, total ->
                    state = DownloadState.Running(done, total)
                }
                file = apk
                state = DownloadState.Ready
                startInstall()
            } catch (e: CancellationException) {
                AppUpdater.apkFile(context).delete()
                state = DownloadState.Idle
                throw e
            } catch (e: Exception) {
                AppUpdater.apkFile(context).delete()
                state = DownloadState.Failed(e.message ?: "Download failed")
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(permissionResult) {
        if (permissionResult) {
            permissionResult = false
            val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.packageManager.canRequestPackageInstalls()
            if (granted && file != null) {
                startInstall()
            } else {
                state = DownloadState.Failed(
                    "Install permission not granted — allow installing apps for " +
                        "MtG Card Tracker, then tap Download & Install again"
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text("Update available: ${release.version} (current v$currentVersion)")
        },
        text = {
            Column {
                Text("Release notes", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Column(
                    Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        release.notes.ifBlank { "No release notes." },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                val failed = state as? DownloadState.Failed
                if (failed != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        failed.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val running = state as? DownloadState.Running
            if (running != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    val label = if (running.total > 0) {
                        val pct = (running.done * 100 / running.total).toInt()
                        "$pct%"
                    } else {
                        "${running.done / 1024} KB"
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            } else if (state is DownloadState.Ready) {
                TextButton(onClick = { startInstall() }) { Text("Install") }
            } else {
                TextButton(
                    onClick = { beginDownload() },
                    enabled = state is DownloadState.Idle || state is DownloadState.Failed
                ) {
                    Text(if (state is DownloadState.Failed) "Try Again" else "Download & Install")
                }
            }
        },
        dismissButton = {
            if (state is DownloadState.Running) {
                TextButton(onClick = {
                    job?.cancel()
                    AppUpdater.apkFile(context).delete()
                    state = DownloadState.Idle
                }) { Text("Cancel") }
            } else {
                TextButton(onClick = onLater) { Text("Later") }
            }
        }
    )
}
