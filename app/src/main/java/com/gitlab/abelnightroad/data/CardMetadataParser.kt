package com.gitlab.abelnightroad.data

/**
 * A single OCR text block with its bounding box as plain ints.
 * Deliberately free of android.graphics types so parsing is JVM-testable.
 */
data class OcrBlock(
    val text: String,
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int
)

data class OcrPage(
    val blocks: List<OcrBlock>,
    val width: Int,
    val height: Int
) {
    val text: String get() = blocks.joinToString("\n") { it.text }
}

data class SetLangInfo(val setCode: String, val language: String)

data class CollectorInfo(val rarity: Char, val raw: String, val normalized: String)

data class CardMetadata(
    val setCode: String,
    val collectorRaw: String,
    val collectorNormalized: String,
    val languageCode: String
)

/**
 * Region-based card identification: the card name lives in the top band of
 * the card, the bottom-left metadata block (rarity + collector number, set +
 * language) is language-independent because set codes and collector numbers
 * are printed in Latin on every modern card. All thresholds are fractional
 * and generous — every function degrades to "" / null instead of failing.
 */
object CardMetadataParser {

    private const val NAME_BOTTOM_FRACTION = 0.20f
    private const val NAME_RIGHT_FRACTION = 0.70f
    private const val META_TOP_FRACTION = 0.82f
    private const val META_RIGHT_FRACTION = 0.45f

    private val collectorRegex = Regex("""^\s*([CUMBRSFT])\s*[-.]?\s*(\d{1,4})\s*$""")
    private val setLangRegex = Regex("""^\s*([A-Z0-9]{2,6})\s*[•·・\-–]?\s*([A-Z]{2,3})\s*$""")

    fun nameRegion(blocks: List<OcrBlock>, height: Int, width: Int): String =
        blocks.filter {
            it.text.isNotBlank() &&
                it.bottom <= NAME_BOTTOM_FRACTION * height &&
                it.right <= NAME_RIGHT_FRACTION * width
        }.joinToString("\n") { it.text.trim() }

    fun metadataRegion(blocks: List<OcrBlock>, height: Int, width: Int): String =
        blocks.filter {
            it.text.isNotBlank() &&
                it.top >= META_TOP_FRACTION * height &&
                it.right <= META_RIGHT_FRACTION * width
        }.joinToString("\n") { it.text.trim() }

    fun parseCollector(line: String): CollectorInfo? {
        val ocrFixed = line.replace('O', '0').replace('o', '0')
        val match = collectorRegex.find(ocrFixed.trim()) ?: return null
        val raw = match.groupValues[2]
        val normalized = raw.trimStart('0').ifEmpty { "0" }
        return CollectorInfo(rarity = match.groupValues[1][0], raw = raw, normalized = normalized)
    }

    fun parseSetLang(line: String): SetLangInfo? {
        val match = setLangRegex.find(line.trim()) ?: return null
        return SetLangInfo(setCode = match.groupValues[1], language = match.groupValues[2])
    }

    fun parseMetadata(blocks: List<OcrBlock>, height: Int, width: Int): CardMetadata? {
        val lines = metadataRegion(blocks, height, width).lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val setLang = lines.firstNotNullOfOrNull { parseSetLang(it) } ?: return null
        val collector = lines.firstNotNullOfOrNull { parseCollector(it) } ?: return null
        return CardMetadata(
            setCode = setLang.setCode,
            collectorRaw = collector.raw,
            collectorNormalized = collector.normalized,
            languageCode = setLang.language
        )
    }

    fun languageFromCode(code: String): String = when (code.uppercase()) {
        "EN" -> "en"
        "JP", "JA" -> "ja"
        "KR", "KO" -> "ko"
        "ZH", "ZHS", "ZHT", "CN" -> "zh"
        else -> code.lowercase()
    }
}
