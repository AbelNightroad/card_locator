package com.gitlab.abelnightroad.data

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
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

    suspend fun recognizeText(inputImage: InputImage): String {
        val chineseText = try {
            chineseRecognizer.process(inputImage).await().text
        } catch (_: Exception) { "" }

        val japaneseText = try {
            japaneseRecognizer.process(inputImage).await().text
        } catch (_: Exception) { "" }

        if (chineseText.length >= japaneseText.length && chineseText.isNotBlank()) return chineseText
        if (japaneseText.isNotBlank()) return japaneseText

        val koreanText = try {
            koreanRecognizer.process(inputImage).await().text
        } catch (_: Exception) { "" }
        if (koreanText.isNotBlank()) return koreanText

        return try {
            latinRecognizer.process(inputImage).await().text
        } catch (_: Exception) { "" }
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
