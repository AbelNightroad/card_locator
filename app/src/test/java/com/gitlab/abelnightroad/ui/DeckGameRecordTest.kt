package com.gitlab.abelnightroad.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckGameRecordTest {

    @Test
    fun `zero games reports zero percent and zeroed record`() {
        val summary = gameRecordSummary(0, 0)
        assertEquals(0, summary.games)
        assertEquals(0, summary.wins)
        assertEquals(0, summary.losses)
        assertEquals(0f, summary.winRatePercent)
        assertTrue(summary.winRatePercent.isFinite())
    }

    @Test
    fun `win rate is computed from games and wins`() {
        val summary = gameRecordSummary(14, 9)
        assertEquals(14, summary.games)
        assertEquals(9, summary.wins)
        assertEquals(5, summary.losses)
        assertEquals(9 * 100f / 14, summary.winRatePercent, 0.001f)
    }

    @Test
    fun `wins equal to games report one hundred percent`() {
        val summary = gameRecordSummary(7, 7)
        assertEquals(0, summary.losses)
        assertEquals(100f, summary.winRatePercent, 0.001f)
    }

    @Test
    fun `wins greater than games are clamped to games`() {
        val summary = gameRecordSummary(3, 9)
        assertEquals(3, summary.wins)
        assertEquals(0, summary.losses)
        assertEquals(100f, summary.winRatePercent, 0.001f)
    }

    @Test
    fun `negative counters are clamped to zero`() {
        val summary = gameRecordSummary(-4, -2)
        assertEquals(0, summary.games)
        assertEquals(0, summary.wins)
        assertEquals(0, summary.losses)
        assertEquals(0f, summary.winRatePercent)

        val mixed = gameRecordSummary(5, -1)
        assertEquals(5, mixed.games)
        assertEquals(0, mixed.wins)
        assertEquals(5, mixed.losses)
        assertEquals(0f, mixed.winRatePercent, 0.001f)
    }

    @Test
    fun `validation matrix for edit dialog`() {
        assertTrue(isValidGameRecord(0, 0))
        assertTrue(isValidGameRecord(14, 9))
        assertTrue(isValidGameRecord(5, 5))
        assertTrue(isValidGameRecord(3, 0))
        assertFalse(isValidGameRecord(3, 4))
        assertFalse(isValidGameRecord(0, 1))
        assertFalse(isValidGameRecord(-1, 0))
        assertFalse(isValidGameRecord(-1, -1))
        assertFalse(isValidGameRecord(5, -1))
    }
}
