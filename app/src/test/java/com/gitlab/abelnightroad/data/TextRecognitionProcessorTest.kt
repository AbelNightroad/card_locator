package com.gitlab.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextRecognitionProcessorTest {

    @Test
    fun `extracts English card name`() {
        val ocr = "Lightning Bolt\nInstant\n{R}\nDamage"
        assertEquals("Lightning Bolt", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `extracts double-faced card name`() {
        val ocr = "Delver of Secrets // Insectile Aberration\nCreature — Human Wizard\n{1}{U}"
        assertEquals("Delver of Secrets // Insectile Aberration", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `strips trailing watermark numbers`() {
        val ocr = "Sol Ring234\nArtifact\n{T}"
        assertEquals("Sol Ring", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `strips leading numbers from OCR noise`() {
        val ocr = "42 Black Lotus\nArtifact\n{0}"
        assertEquals("Black Lotus", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `skips mana cost line`() {
        val ocr = "{2}{U}{U}\nCounterspell\nInstant"
        assertEquals("Counterspell", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `skips type line`() {
        val ocr = "Creature — Elf Warrior\nLlanowar Elves\n{G}"
        assertEquals("Llanowar Elves", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `skips Legendary supertype`() {
        val ocr = "Legendary Creature — Dragon\nArcades Sabboth\n{3}{G}{G}{W}{W}"
        assertEquals("Arcades Sabboth", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `extracts Portuguese card name`() {
        val ocr = "Relâmpago\nInstantâneo\n{R}"
        assertEquals("Relâmpago", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `extracts Japanese card name with kanji`() {
        val ocr = "雷撃\nインスタント\n{R}"
        assertEquals("雷撃", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `extracts Chinese card name`() {
        val ocr = "闪电击\n瞬间\n{R}"
        assertEquals("闪电击", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `returns null for empty text`() {
        assertNull(TextRecognitionProcessor.extractCardName(""))
        assertNull(TextRecognitionProcessor.extractCardName("   \n  \n  "))
    }

    @Test
    fun `returns first line when no good match found`() {
        val ocr = "XYZ"
        assertEquals("XYZ", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `strips asterisk watermark artifacts`() {
        val ocr = "Dark Ritual***\nInstant\n{B}"
        assertEquals("Dark Ritual", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `handles single character line by skipping`() {
        val ocr = "A\nLightning Bolt\nInstant"
        assertEquals("Lightning Bolt", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `handles OCR with extra whitespace`() {
        val ocr = "  Lightning Bolt  \n  Instant  \n  {R}  "
        assertEquals("Lightning Bolt", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `extracts Sol Ring from realistic OCR output`() {
        val ocr = """
            Sol Ring47
            Artifact
            {4}
            Tap: Add {C}{C}.
        """.trimIndent()
        assertEquals("Sol Ring", TextRecognitionProcessor.extractCardName(ocr))
    }

    @Test
    fun `extracts card name from messy multi-line OCR`() {
        val ocr = """
            3
            Black Lotus
            232
            Artifact
            {0}
            Tap, Sacrifice: Add three mana of any one color.
        """.trimIndent()
        assertEquals("Black Lotus", TextRecognitionProcessor.extractCardName(ocr))
    }
}
