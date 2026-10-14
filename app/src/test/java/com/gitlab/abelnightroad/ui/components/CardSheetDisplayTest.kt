package com.gitlab.abelnightroad.ui.components

import com.gitlab.abelnightroad.data.CardConditions
import org.junit.Assert.assertEquals
import org.junit.Test

class CardSheetDisplayTest {

    @Test
    fun `added date parses full iso timestamp`() {
        assertEquals("Added on 2 Jan 2026", formatAddedDate("2026-01-02T14:30:00"))
    }

    @Test
    fun `added date parses date-only value`() {
        assertEquals("Added on 5 Mar 2026", formatAddedDate("2026-03-05"))
    }

    @Test
    fun `added date falls back to raw value`() {
        assertEquals("Added on garbage", formatAddedDate("garbage"))
    }

    @Test
    fun `finish display formats canonical values`() {
        assertEquals("Normal", displayFinish("normal"))
        assertEquals("Foil", displayFinish("foil"))
        assertEquals("Etched", displayFinish("etched"))
        assertEquals("Etched Foil", displayFinish("etched foil"))
        assertEquals("Etched Foil", displayFinish("FOIL ETCHED"))
        assertEquals("Custom", displayFinish("Custom"))
    }

    @Test
    fun `condition display formats known grades`() {
        assertEquals("Mint", CardConditions.displayName("M"))
        assertEquals("Near Mint", CardConditions.displayName("NM"))
        assertEquals("Slightly Played", CardConditions.displayName("SP"))
        assertEquals("Played", CardConditions.displayName("PL"))
        assertEquals("Heavily Played", CardConditions.displayName("HP"))
        assertEquals("Damaged", CardConditions.displayName("D"))
        assertEquals("PSA 10", CardConditions.displayName("PSA 10"))
    }
}
