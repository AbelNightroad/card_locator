package com.gitlab.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GitHubReleaseClientTest {

    private fun fixture(name: String): String =
        javaClass.getResourceAsStream("/fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `latest release parses tag notes and apk url`() {
        val release = GitHubReleaseClient.parseRelease(fixture("github_release_latest.json"))
        assertEquals("v1.6.0", release.version)
        assertTrue(release.notes.contains("Deck card sheet"))
        assertTrue(
            release.apkUrl.endsWith("MtGCardTracker-1.6.0.apk")
        )
    }

    @Test
    fun `release without apk asset throws explicit error`() {
        val error = assertThrows(IOException::class.java) {
            GitHubReleaseClient.parseRelease(
                """{"tag_name":"v1.6.0","assets":[{"name":"notes.txt","browser_download_url":"https://x/notes.txt"}]}"""
            )
        }
        assertTrue(error.message!!.contains("no APK"))
    }

    @Test
    fun `rate limit body maps to rate-limit message`() {
        val body = fixture("github_rate_limit.json")
        val error = assertThrows(IOException::class.java) {
            GitHubReleaseClient.parseRelease(body)
        }
        assertEquals("GitHub rate limit — try again later", error.message)
        assertEquals(
            "GitHub rate limit — try again later",
            GitHubReleaseClient.apiErrorMessage(403, body)
        )
    }

    @Test
    fun `non-200 body without message reports http code`() {
        assertEquals(
            "GitHub API returned HTTP 500",
            GitHubReleaseClient.apiErrorMessage(500, "boom")
        )
    }

    @Test
    fun `malformed json throws explicit error`() {
        val error = assertThrows(IOException::class.java) {
            GitHubReleaseClient.parseRelease("not json at all")
        }
        assertTrue(error.message!!.contains("Malformed"))
    }

    @Test
    fun `newer minor version is detected with v prefix stripped`() {
        assertTrue(GitHubReleaseClient.isNewerThan("1.5.2", "v1.6.0"))
        assertTrue(GitHubReleaseClient.isNewerThan("v1.5.2", "1.6.0"))
    }

    @Test
    fun `multi-digit segments compare numerically not lexically`() {
        assertTrue(GitHubReleaseClient.isNewerThan("1.9.3", "1.10.0"))
        assertFalse(GitHubReleaseClient.isNewerThan("1.10.0", "1.9.3"))
    }

    @Test
    fun `same or older versions are not newer`() {
        assertFalse(GitHubReleaseClient.isNewerThan("1.5.2", "v1.5.2"))
        assertFalse(GitHubReleaseClient.isNewerThan("1.5.2", "v1.4.9"))
    }

    @Test
    fun `missing segments are treated as zero`() {
        assertTrue(GitHubReleaseClient.isNewerThan("1.5", "1.5.1"))
        assertFalse(GitHubReleaseClient.isNewerThan("1.5.1", "1.5"))
    }
}
