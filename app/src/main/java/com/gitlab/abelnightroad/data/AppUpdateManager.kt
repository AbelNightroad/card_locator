package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the app-update state: a silent weekly launch check and an explicit
 * manual check from Settings. The pending release is a Compose-observable
 * [StateFlow] so `AppNavigation` can host the one-shot update dialog.
 */
class AppUpdateManager(private val settings: SettingsStore) {

    sealed interface CheckResult {
        data class UpToDate(val version: String) : CheckResult
        data class UpdateFound(val release: LatestRelease) : CheckResult
        data class Failed(val message: String) : CheckResult
    }

    private val _pending = MutableStateFlow<LatestRelease?>(null)
    val pending: StateFlow<LatestRelease?> = _pending.asStateFlow()

    /**
     * Weekly on-launch check: runs only after 7 days since the last check,
     * persists the timestamp regardless of the outcome and never surfaces
     * errors (a background failure must not interrupt startup).
     */
    suspend fun checkWeekly() {
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck() < SEVEN_DAYS_MS) return
        settings.setLastUpdateCheck(now)
        when (val result = check()) {
            is CheckResult.UpdateFound -> {
                if (result.release.version != settings.updateDismissedVersion()) {
                    _pending.value = result.release
                }
            }
            else -> Unit
        }
    }

    /**
     * Manual check from Settings — bypasses the 7-day timestamp (but advances
     * it on success) and ignores the dismissed version (the user asked).
     */
    suspend fun checkManual(): CheckResult {
        val result = check()
        if (result !is CheckResult.Failed) {
            settings.setLastUpdateCheck(System.currentTimeMillis())
        }
        if (result is CheckResult.UpdateFound) {
            _pending.value = result.release
        }
        return result
    }

    /** "Later": suppresses this version for future weekly checks. */
    suspend fun dismissPending() {
        val release = _pending.value ?: return
        settings.setUpdateDismissedVersion(release.version)
        _pending.value = null
    }

    /** Hides the dialog without storing a dismissal (e.g. after install started). */
    fun clearPending() {
        _pending.value = null
    }

    private suspend fun check(): CheckResult {
        val release = try {
            GitHubReleaseClient.fetchLatest()
        } catch (e: Exception) {
            return CheckResult.Failed(e.message ?: "Update check failed")
        }
        return if (GitHubReleaseClient.isNewerThan(BuildConfig.VERSION_NAME, release.version)) {
            CheckResult.UpdateFound(release)
        } else {
            CheckResult.UpToDate(BuildConfig.VERSION_NAME)
        }
    }

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
