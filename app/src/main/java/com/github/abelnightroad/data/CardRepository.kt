package com.github.abelnightroad.data

import android.content.Context
import com.github.abelnightroad.db.AppDatabase
import com.github.abelnightroad.db.CardDao
import com.github.abelnightroad.db.CardEntity
import com.github.abelnightroad.db.CardSearchResult
import com.github.abelnightroad.db.MultiCopyCard
import com.github.abelnightroad.db.TagCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream

class CardRepository(private val dao: CardDao) {

    val tagCounts: Flow<List<TagCount>> = dao.tagCounts()
    val multiCopyCards: Flow<List<MultiCopyCard>> = dao.multiCopyCards()

    fun cardsByTag(tag: String): Flow<List<CardSearchResult>> = dao.cardsByTag(tag)
    fun searchByName(query: String): Flow<List<CardSearchResult>> = dao.searchByName(query)

    suspend fun isEmpty(): Boolean = dao.count() == 0

    /** Inserts a single manually-added card. */
    suspend fun addCard(card: CardEntity) = dao.insert(card)

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
            return CardRepository(db.cardDao())
        }
    }
}
