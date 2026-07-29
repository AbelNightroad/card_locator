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

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun getAllCards(): List<CardEntity> = dao.getAll()

    /** Replaces the entire collection with a new set of cards. */
    suspend fun replaceAll(cards: List<CardEntity>) {
        dao.clear()
        if (cards.isNotEmpty()) dao.insertAll(cards)
    }

    /** Inserts a single manually-added card. */
    suspend fun addCard(card: CardEntity) = dao.insert(card)

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
