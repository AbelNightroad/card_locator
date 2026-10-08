package com.gitlab.abelnightroad.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardMetadata
import com.gitlab.abelnightroad.data.CardMetadataParser
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScanRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.TextRecognitionProcessor
import com.gitlab.abelnightroad.db.ScannedCardEntity
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ScanOverlayState {
    data object Scanning : ScanOverlayState
    data class Found(val name: String) : ScanOverlayState
    data class Error(val message: String) : ScanOverlayState
}

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val _lastScan = MutableStateFlow<ScanOverlayState?>(null)
    val lastScan: StateFlow<ScanOverlayState?> = _lastScan.asStateFlow()

    val scannedCount: StateFlow<Int> = _sessionId
        .flatMapLatest { sid ->
            if (sid == null) flowOf(emptyList<ScannedCardEntity>())
            else scanRepository.getScannedCards(sid)
        }
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
            _lastScan.value = ScanOverlayState.Scanning

            try {
                if (_sessionId.value == null) {
                    _sessionId.value = scanRepository.createSession()
                }
                val sid = _sessionId.value ?: return@launch

                val (image, page) = withContext(Dispatchers.Default) {
                    val img = InputImage.fromFilePath(
                        processor.context,
                        Uri.parse("file://$filePath")
                    )
                    img to processor.recognizeTextRegions(img)
                }

                val fullText = page.text
                if (fullText.isBlank()) {
                    fail("No text detected from image")
                    return@launch
                }

                var meta = CardMetadataParser.parseMetadata(page.blocks, page.height, page.width)
                var card = meta?.let { lookupByMetadata(it) }
                var cardName: String? = null

                if (card == null) {
                    val nameText = CardMetadataParser.nameRegion(page.blocks, page.height, page.width)
                        .ifBlank { fullText }
                    val name = TextRecognitionProcessor.extractCardName(nameText)
                    cardName = name
                    if (name != null) {
                        card = withContext(Dispatchers.IO) { scryfall.lookupByNameResilient(name) }
                    }
                }

                if (card == null) {
                    val latinPage = withContext(Dispatchers.Default) { processor.recognizeLatinRegions(image) }
                    if (latinPage.blocks.isNotEmpty()) {
                        val latinMeta = CardMetadataParser.parseMetadata(
                            latinPage.blocks, latinPage.height, latinPage.width
                        )
                        if (latinMeta != null && meta == null) meta = latinMeta
                        card = latinMeta?.let { lookupByMetadata(it) }
                        if (card == null) {
                            val nameText = CardMetadataParser.nameRegion(
                                latinPage.blocks, latinPage.height, latinPage.width
                            ).ifBlank { latinPage.text }
                            val latinName = TextRecognitionProcessor.extractCardName(nameText)
                            if (latinName != null) {
                                card = withContext(Dispatchers.IO) { scryfall.lookupByNameResilient(latinName) }
                                if (cardName == null) cardName = latinName
                            }
                        }
                    }
                }

                if (card == null) {
                    fail("Card not found: ${cardName ?: "card"}")
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
                        language = meta?.languageCode?.let { CardMetadataParser.languageFromCode(it) }
                            ?: detectLanguage(fullText)
                    )
                )
                _addedCount.value = _addedCount.value + 1
                _lastScan.value = ScanOverlayState.Found(card.name)
            } catch (e: Exception) {
                fail("Error: ${e.message}")
            } finally {
                _isProcessing.value = false
                File(filePath).delete()
            }
        }
    }

    private suspend fun lookupByMetadata(meta: CardMetadata) = withContext(Dispatchers.IO) {
        scryfall.lookupBySetAndCollector(meta.setCode, meta.collectorRaw)
            ?: scryfall.lookupBySetAndCollector(meta.setCode, meta.collectorNormalized)
    }

    private fun fail(message: String) {
        _lastError.value = message
        _lastScan.value = ScanOverlayState.Error(message)
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
