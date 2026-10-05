package com.gitlab.abelnightroad.data

import android.content.Context
import com.gitlab.abelnightroad.db.AppDatabase
import com.gitlab.abelnightroad.db.CardDao
import com.gitlab.abelnightroad.db.CardEntity
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.db.MultiCopyCard
import com.gitlab.abelnightroad.db.TagCount
import com.gitlab.abelnightroad.db.TagDao
import com.gitlab.abelnightroad.db.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream

class CardRepository(private val dao: CardDao, private val tagDao: TagDao) {

    val tagCounts: Flow<List<TagCount>> = dao.tagCounts()
    val multiCopyCards: Flow<List<MultiCopyCard>> = dao.multiCopyCards()

    fun cardsByTag(tag: String): Flow<List<CardSearchResult>> = dao.cardsByTag(tag)
    fun searchByName(query: String): Flow<List<CardSearchResult>> = dao.searchByName(query)
    fun searchAdvanced(query: String, colors: Set<String>, type: String?, rarity: String?): Flow<List<CardSearchResult>> =
        dao.searchAdvanced(query, colors.joinToString("").ifEmpty { null }, type, rarity)

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun getAllCards(): List<CardEntity> = dao.getAll()

    /** Replaces the entire collection with a new set of cards. */
    suspend fun replaceAll(cards: List<CardEntity>) {
        dao.clear()
        if (cards.isNotEmpty()) dao.insertAll(cards)
    }

    /** Inserts a single manually-added card. */
    suspend fun addCard(card: CardEntity) = dao.insert(card)

    /** Bulk insert without clearing existing data (for 3rd-party imports). */
    suspend fun insertAll(cards: List<CardEntity>) = dao.insertAll(cards)

    /**
     * Inserts [cards] into their tags, merging duplicates: when a row with the
     * same tag, name, set, collector number and finish already exists and every
     * other field matches, the quantities are summed instead of adding a second
     * row. Rows that differ anywhere else stay separate cards.
     */
    suspend fun insertMergingDuplicates(cards: List<CardEntity>) {
        for (card in cards) {
            val duplicate = dao.findByDuplicateKey(
                card.tag, card.name, card.setCode, card.collectorNumber, card.foil
            ).firstOrNull { sameCardInfo(it, card) }
            if (duplicate != null) dao.addQuantity(duplicate.id, card.quantity)
            else dao.insert(card)
        }
    }

    /** True when both rows describe the same physical card apart from quantity/id/added. */
    private fun sameCardInfo(a: CardEntity, b: CardEntity): Boolean =
        a.name == b.name &&
            a.setName == b.setName &&
            a.rarity == b.rarity &&
            a.manaBoxId == b.manaBoxId &&
            a.scryfallId == b.scryfallId &&
            a.purchasePrice == b.purchasePrice &&
            a.misprint == b.misprint &&
            a.altered == b.altered &&
            a.condition == b.condition &&
            a.language == b.language &&
            a.purchasePriceCurrency == b.purchasePriceCurrency


    suspend fun incrementQuantity(id: Long) = dao.incrementQuantity(id)

    suspend fun decrementQuantity(id: Long) {
        dao.deleteIfQuantityOne(id)
        dao.decrementQuantity(id)
    }

    suspend fun deleteCard(id: Long) = dao.deleteById(id)

    suspend fun deleteByTag(tag: String) {
        tagDao.deleteCardsByTag(tag)
        tagDao.delete(tag)
    }

    suspend fun renameTag(oldTag: String, newTag: String) {
        dao.renameTag(oldTag, newTag)
        tagDao.rename(oldTag, newTag)
    }

    suspend fun createTag(tag: String) = tagDao.insert(TagEntity(tag))

    /**
     * Imports a ManaBox CSV, tagging every row with [tag] (a storage location).
     * Rows are appended; existing rows for the same tag are replaced on re-import.
     */
    suspend fun importCsv(input: InputStream, tag: String): CsvImport.Result =
        withContext(Dispatchers.IO) {
            val result = CsvImport.parse(input, tag)
            dao.insertAll(result.cards)
            result
        }

    companion object {
        fun create(context: Context): CardRepository {
            val db = AppDatabaseProvider.get(context)
            return CardRepository(db.cardDao(), db.tagDao())
        }
    }
}
