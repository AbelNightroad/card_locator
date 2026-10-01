package com.gitlab.abelnightroad.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportSourceDetectTest {

    @Test
    fun `detects source from full deck urls`() {
        assertEquals(
            ImportSource.MOXFIELD,
            ImportSource.detect("https://www.moxfield.com/decks/aSGf97rAHkKrxR_5OfihbA")
        )
        assertEquals(
            ImportSource.EDHREC,
            ImportSource.detect("https://edhrec.com/deckpreview/4Rm5X7VN7c_H1V33ojqsAw")
        )
        assertEquals(
            ImportSource.EDHREC,
            ImportSource.detect("https://edhrec.com/average-decks/tidus-yunas-guardian")
        )
        assertEquals(
            ImportSource.ARCHIDEKT,
            ImportSource.detect("https://archidekt.com/decks/26960307/super_friends")
        )
        assertEquals(
            ImportSource.MTG_GOLDFISH,
            ImportSource.detect("https://www.mtggoldfish.com/deck/7979312#paper")
        )
        assertEquals(
            ImportSource.TAPPED_OUT,
            ImportSource.detect("https://tappedout.net/mtg-decks/the-pharaohs-flare/")
        )
    }

    @Test
    fun `detects scheme-less subdomain-less and uppercase urls`() {
        assertEquals(ImportSource.MOXFIELD, ImportSource.detect("moxfield.com/decks/xyz"))
        assertEquals(ImportSource.EDHREC, ImportSource.detect("http://edhrec.com/average-decks/foo"))
        assertEquals(ImportSource.MOXFIELD, ImportSource.detect("HTTPS://WWW.MOXFIELD.COM/decks/X"))
        assertEquals(ImportSource.ARCHIDEKT, ImportSource.detect("  https://archidekt.com/decks/123  "))
    }

    @Test
    fun `unknown hosts and plain text return null`() {
        assertNull(ImportSource.detect("https://example.com/decks/1"))
        assertNull(ImportSource.detect("4 Lightning Bolt (MHR) 123"))
        assertNull(ImportSource.detect(""))
        assertNull(ImportSource.detect("   "))
    }
}
