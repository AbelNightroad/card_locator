package com.gitlab.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardMetadataParserTest {

    private val w = 1000
    private val h = 1400

    private fun block(text: String, top: Int, bottom: Int, left: Int, right: Int) =
        OcrBlock(text = text, top = top, bottom = bottom, left = left, right = right)

    @Test
    fun `english card name and metadata regions split correctly`() {
        val blocks = listOf(
            block("Lightning Bolt", top = 60, bottom = 140, left = 70, right = 420),
            block("{R}", top = 60, bottom = 140, left = 850, right = 950),
            block("Instant", top = 400, bottom = 450, left = 70, right = 300),
            block("C 161\nLEA • EN", top = 1200, bottom = 1320, left = 70, right = 380)
        )
        assertEquals("Lightning Bolt", CardMetadataParser.nameRegion(blocks, h, w))

        val meta = CardMetadataParser.parseMetadata(blocks, h, w)!!
        assertEquals("LEA", meta.setCode)
        assertEquals("161", meta.collectorRaw)
        assertEquals("161", meta.collectorNormalized)
        assertEquals("EN", meta.languageCode)
    }

    @Test
    fun `japanese card metadata resolves to canonical identity`() {
        val blocks = listOf(
            block("雷撃", top = 60, bottom = 150, left = 70, right = 300),
            block("インスタント", top = 400, bottom = 450, left = 70, right = 350),
            block("U 0040\nFCA • JP", top = 1200, bottom = 1320, left = 70, right = 380)
        )
        assertEquals("雷撃", CardMetadataParser.nameRegion(blocks, h, w))

        val meta = CardMetadataParser.parseMetadata(blocks, h, w)!!
        assertEquals("FCA", meta.setCode)
        assertEquals("0040", meta.collectorRaw)
        assertEquals("40", meta.collectorNormalized)
        assertEquals("ja", CardMetadataParser.languageFromCode(meta.languageCode))
    }

    @Test
    fun `chinese card metadata resolves to canonical identity`() {
        val blocks = listOf(
            block("闪电击", top = 60, bottom = 150, left = 70, right = 300),
            block("瞬间", top = 400, bottom = 450, left = 70, right = 250),
            block("C 161\nBLB • ZHS", top = 1200, bottom = 1320, left = 70, right = 380)
        )
        val meta = CardMetadataParser.parseMetadata(blocks, h, w)!!
        assertEquals("BLB", meta.setCode)
        assertEquals("161", meta.collectorRaw)
        assertEquals("zh", CardMetadataParser.languageFromCode(meta.languageCode))
    }

    @Test
    fun `collector numbers keep raw and leading-zero-stripped variants`() {
        assertEquals("0040", CardMetadataParser.parseCollector("U 0040")!!.raw)
        assertEquals("40", CardMetadataParser.parseCollector("U 0040")!!.normalized)
        assertEquals("40", CardMetadataParser.parseCollector("U 40")!!.raw)
        assertEquals("40", CardMetadataParser.parseCollector("U 40")!!.normalized)
        assertEquals("0", CardMetadataParser.parseCollector("R 000")!!.normalized)
    }

    @Test
    fun `collector parsing tolerates O and 0 confusion`() {
        val fromLetterO = CardMetadataParser.parseCollector("C O161")!!
        assertEquals("0161", fromLetterO.raw)
        assertEquals("161", fromLetterO.normalized)

        val fromLowerO = CardMetadataParser.parseCollector("C 1o61")!!
        assertEquals("1061", fromLowerO.raw)
        assertEquals("1061", fromLowerO.normalized)
    }

    @Test
    fun `collector parsing extracts rarity letter`() {
        assertEquals('C', CardMetadataParser.parseCollector("C 161")!!.rarity)
        assertEquals('M', CardMetadataParser.parseCollector("M 12")!!.rarity)
        assertEquals('U', CardMetadataParser.parseCollector("U-40")!!.rarity)
        assertNull(CardMetadataParser.parseCollector("Lightning Bolt"))
        assertNull(CardMetadataParser.parseCollector("C 161 extra"))
    }

    @Test
    fun `set and language line parses with and without separator`() {
        val withSep = CardMetadataParser.parseSetLang("FCA • EN")!!
        assertEquals("FCA", withSep.setCode)
        assertEquals("EN", withSep.language)

        val noSep = CardMetadataParser.parseSetLang("MH2 EN")!!
        assertEquals("MH2", noSep.setCode)
        assertEquals("EN", noSep.language)

        assertEquals("ZHS", CardMetadataParser.parseSetLang("BLB - ZHS")?.language)
        assertNull(CardMetadataParser.parseSetLang("Lightning Bolt"))
        assertNull(CardMetadataParser.parseSetLang("FCA • EN • Extra"))
    }

    @Test
    fun `artist line is excluded from the metadata region by position`() {
        val blocks = listOf(
            block("Lightning Bolt", top = 60, bottom = 140, left = 70, right = 420),
            block("C 161\nLEA • EN", top = 1200, bottom = 1280, left = 70, right = 380),
            block("by Margaret Brainard Cooper", top = 1210, bottom = 1260, left = 600, right = 940),
            block("™ & © 1993 Wizards of the Coast", top = 1330, bottom = 1370, left = 500, right = 940)
        )
        val region = CardMetadataParser.metadataRegion(blocks, h, w)
        assertTrue(region.contains("C 161"))
        assertTrue(region.contains("LEA • EN"))
        assertTrue(!region.contains("Margaret"))
        assertTrue(!region.contains("Wizards"))

        val meta = CardMetadataParser.parseMetadata(blocks, h, w)!!
        assertEquals("LEA", meta.setCode)
    }

    @Test
    fun `blocks above the metadata band are ignored`() {
        val blocks = listOf(
            block("Creature — Human Wizard", top = 1000, bottom = 1050, left = 70, right = 380)
        )
        assertEquals("", CardMetadataParser.metadataRegion(blocks, h, w))
        assertNull(CardMetadataParser.parseMetadata(blocks, h, w))
    }

    @Test
    fun `missing metadata falls back to the name region`() {
        val blocks = listOf(
            block("Sol Ring", top = 60, bottom = 140, left = 70, right = 300),
            block("Artifact", top = 400, bottom = 450, left = 70, right = 250)
        )
        assertNull(CardMetadataParser.parseMetadata(blocks, h, w))
        assertEquals("Sol Ring", CardMetadataParser.nameRegion(blocks, h, w))
    }

    @Test
    fun `name region beats first line when the first line is a watermark`() {
        val blocks = listOf(
            block("234", top = 40, bottom = 100, left = 800, right = 950),
            block("Delver of Secrets // Insectile Aberration", top = 60, bottom = 150, left = 70, right = 680),
            block("Creature — Human Wizard", top = 400, bottom = 450, left = 70, right = 420)
        )
        val nameRegion = CardMetadataParser.nameRegion(blocks, h, w)
        assertEquals("Delver of Secrets // Insectile Aberration", nameRegion)
        assertTrue(!nameRegion.contains("234"))
    }

    @Test
    fun `language codes normalize to the app scheme`() {
        assertEquals("en", CardMetadataParser.languageFromCode("EN"))
        assertEquals("ja", CardMetadataParser.languageFromCode("JP"))
        assertEquals("ja", CardMetadataParser.languageFromCode("JA"))
        assertEquals("ko", CardMetadataParser.languageFromCode("KR"))
        assertEquals("zh", CardMetadataParser.languageFromCode("ZHS"))
        assertEquals("zh", CardMetadataParser.languageFromCode("ZHT"))
        assertEquals("de", CardMetadataParser.languageFromCode("DE"))
    }
}
