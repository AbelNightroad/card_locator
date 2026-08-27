package com.gitlab.abelnightroad.data

import android.content.Context
import com.gitlab.abelnightroad.db.AppDatabase
import com.gitlab.abelnightroad.db.CardEntity
import com.gitlab.abelnightroad.db.ScanSessionDao
import com.gitlab.abelnightroad.db.ScanSessionEntity
import com.gitlab.abelnightroad.db.ScannedCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class ScanRepository(private val dao: ScanSessionDao) {

    fun getScannedCards(sessionId: Long): Flow<List<ScannedCardEntity>> =
        dao.getCardsBySession(sessionId)

    fun getAllSessions(): Flow<List<ScanSessionEntity>> = dao.getAllSessions()

    suspend fun createSession(): Long {
        return dao.insertSession(ScanSessionEntity(createdAt = LocalDateTime.now().toString()))
    }

    suspend fun addScannedCard(sessionId: Long, card: ScannedCardEntity): Long {
        return dao.insertCard(card.copy(sessionId = sessionId))
    }

    suspend fun deleteScannedCard(cardId: Long) = dao.deleteCard(cardId)

    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    suspend fun addToCollection(
        sessionId: Long,
        tag: String,
        cardRepository: CardRepository
    ): Int = withContext(Dispatchers.IO) {
        val cards = dao.getCardsBySessionOnce(sessionId)
        val entities = cards.map { scanned ->
            CardEntity(
                name = scanned.name,
                setCode = scanned.setCode,
                setName = scanned.setName,
                collectorNumber = scanned.collectorNumber,
                foil = "normal",
                rarity = scanned.rarity,
                quantity = 1,
                manaBoxId = "",
                scryfallId = scanned.scryfallId,
                purchasePrice = scanned.priceUsd,
                misprint = false,
                altered = false,
                condition = "Near Mint",
                language = scanned.language,
                purchasePriceCurrency = "USD",
                added = LocalDateTime.now().toString(),
                tag = tag.ifBlank { "Scan" }
            )
        }
        cardRepository.insertAll(entities)
        entities.size
    }

    companion object {
        fun create(context: Context): ScanRepository {
            val db = AppDatabaseProvider.get(context)
            return ScanRepository(db.scanSessionDao())
        }
    }
}
