package com.gitlab.abelnightroad.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScanRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.TextRecognitionProcessor
import com.gitlab.abelnightroad.db.ScannedCardEntity
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val cardRepository: CardRepository,
    private val scryfall: ScryfallRepository,
    private val processor: TextRecognitionProcessor
) : ViewModel() {

    private val _sessionId = MutableStateFlow<Long?>(null)
    val sessionId: StateFlow<Long?> = _sessionId.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _addedCount = MutableStateFlow(0)
    val addedCount: StateFlow<Int> = _addedCount.asStateFlow()

    suspend fun ensureSession(): Long {
        _sessionId.value?.let { return it }
        val sid = scanRepository.createSession()
        _sessionId.value = sid
        return sid
    }

    fun processImage(filePath: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _lastError.value = null

            try {
                if (_sessionId.value == null) {
                    _sessionId.value = scanRepository.createSession()
                }
                val sid = _sessionId.value ?: return@launch

                val ocrText = withContext(Dispatchers.Default) {
                    val image = InputImage.fromFilePath(
                        processor.context,
                        Uri.parse("file://$filePath")
                    )
                    processor.recognizeText(image)
                }

                if (ocrText.isBlank()) {
                    _lastError.value = "No text detected from image"
                    _isProcessing.value = false
                    return@launch
                }

                val cardName = TextRecognitionProcessor.extractCardName(ocrText)
                if (cardName == null) {
                    _lastError.value = "Could not detect card name from image"
                    _isProcessing.value = false
                    return@launch
                }

                val card = withContext(Dispatchers.IO) {
                    scryfall.lookupByNameResilient(cardName)
                }
                if (card == null) {
                    _lastError.value = "Card not found: $cardName"
                    _isProcessing.value = false
                    return@launch
                }

                scanRepository.addScannedCard(
                    sessionId = sid,
                    card = ScannedCardEntity(
                        sessionId = sid,
                        name = card.name,
                        setCode = card.setCode,
                        setName = card.setName,
                        collectorNumber = card.collectorNumber,
                        rarity = card.rarity,
                        manaCost = card.manaCost,
                        typeLine = card.typeLine,
                        oracleText = card.oracleText,
                        colorIdentity = card.colorIdentity,
                        scryfallId = card.id,
                        priceUsd = card.priceUsd ?: 0.0,
                        language = detectLanguage(ocrText)
                    )
                )
                _addedCount.value = _addedCount.value + 1
            } catch (e: Exception) {
                _lastError.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun getScannedCards(sessionId: Long) = scanRepository.getScannedCards(sessionId)

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            scanRepository.deleteScannedCard(cardId)
        }
    }

    fun addToCollection(tag: String) {
        val sid = _sessionId.value ?: return
        viewModelScope.launch {
            scanRepository.addToCollection(sid, tag, cardRepository)
            _addedCount.value = 0
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    private fun detectLanguage(ocrText: String): String {
        val hasHiragana = ocrText.contains(Regex("[\\u3040-\\u309F]"))
        val hasKatakana = ocrText.contains(Regex("[\\u30A0-\\u30FF]"))
        val hasKanji = ocrText.contains(Regex("[\\u4E00-\\u9FFF]"))
        val hasHangul = ocrText.contains(Regex("[\\uAC00-\\uD7AF]"))

        if (hasHiragana || hasKatakana) return "ja"
        if (hasKanji) return "zh"
        if (hasHangul) return "ko"
        return "en"
    }
}
