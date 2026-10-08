package com.gitlab.abelnightroad.data

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class TextRecognitionProcessor(val context: Context) {

    private val latinRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val chineseRecognizer: TextRecognizer =
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    private val japaneseRecognizer: TextRecognizer =
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    private val koreanRecognizer: TextRecognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(inputImage: InputImage): String =
        recognizeTextRegions(inputImage).text

    /** Runs the ordered recognizer pass and keeps block text + bounding boxes. */
    suspend fun recognizeTextRegions(inputImage: InputImage): OcrPage {
        val chinese = safeProcess(chineseRecognizer, inputImage)
        val japanese = safeProcess(japaneseRecognizer, inputImage)
        val chineseText = chinese?.text.orEmpty()
        val japaneseText = japanese?.text.orEmpty()

        val chosen = when {
            chinese != null && chineseText.isNotBlank() && chineseText.length >= japaneseText.length -> chinese
            japanese != null && japaneseText.isNotBlank() -> japanese
            else -> safeProcess(koreanRecognizer, inputImage)?.takeIf { it.text.isNotBlank() }
                ?: safeProcess(latinRecognizer, inputImage)
        }
        return toPage(chosen, inputImage)
    }

    /** Cheap Latin-only retry used when the ordered pass misses the metadata. */
    suspend fun recognizeLatinRegions(inputImage: InputImage): OcrPage =
        toPage(safeProcess(latinRecognizer, inputImage), inputImage)

    private suspend fun safeProcess(recognizer: TextRecognizer, inputImage: InputImage): Text? =
        try {
            recognizer.process(inputImage).await()
        } catch (_: Exception) {
            null
        }

    private fun toPage(result: Text?, inputImage: InputImage): OcrPage {
        if (result == null) return OcrPage(emptyList(), inputImage.width, inputImage.height)
        val blocks = result.textBlocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            OcrBlock(
                text = block.text,
                top = box.top,
                bottom = box.bottom,
                left = box.left,
                right = box.right
            )
        }
        return OcrPage(blocks, inputImage.width, inputImage.height)
    }

    fun close() {
        latinRecognizer.close()
        chineseRecognizer.close()
        japaneseRecognizer.close()
        koreanRecognizer.close()
    }

    companion object {
        fun extractCardName(ocrText: String): String? {
            val lines = ocrText.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (lines.isEmpty()) return null

            for (line in lines) {
                val cleaned = line
                    .replace(Regex("\\d{2,}$"), "")
                    .replace(Regex("\\*+$"), "")
                    .replace(Regex("^\\d+"), "")
                    .trim()

                if (cleaned.isBlank()) continue
                if (cleaned.matches(Regex("^[\\d\\s{/}UWBRGSCXP]+$"))) continue
                if (cleaned.length < 2) continue
                if (cleaned.contains("Creature", ignoreCase = true)) continue
                if (cleaned.contains("Instant", ignoreCase = true)) continue
                if (cleaned.contains("Sorcery", ignoreCase = true)) continue
                if (cleaned.contains("Enchantment", ignoreCase = true)) continue
                if (cleaned.contains("Artifact", ignoreCase = true)) continue
                if (cleaned.contains("Planeswalker", ignoreCase = true)) continue
                if (cleaned.contains("Land", ignoreCase = true)) continue
                if (cleaned.contains("Battle", ignoreCase = true)) continue
                if (cleaned.contains("Legendary", ignoreCase = true)) continue
                if (cleaned.contains("Token", ignoreCase = true)) continue

                return cleaned
            }

            return lines.firstOrNull()
        }
    }
}
