package com.gitlab.abelnightroad.data

import android.content.Context
import com.gitlab.abelnightroad.db.AppDatabase
import com.gitlab.abelnightroad.db.ScryfallCardDao
import com.gitlab.abelnightroad.db.ScryfallCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class ScryfallRepository(
    private val dao: ScryfallCardDao,
    private val settings: SettingsStore
) {

    fun autocomplete(query: String): Flow<List<ScryfallCardEntity>> = dao.autocomplete(query)

    suspend fun lookupByName(name: String): ScryfallCardEntity? = dao.byName(name)

    suspend fun lookupByNameResilient(name: String): ScryfallCardEntity? {
        val trimmed = name.trim()
        dao.byName(trimmed)?.let { return it }
        if (trimmed.contains(" // ")) {
            for (part in trimmed.split(" // ").map { it.trim() }.filter { it.isNotBlank() }) {
                dao.byName(part)?.let { return it }
            }
        }
        return null
    }

    suspend fun lookupById(id: String): ScryfallCardEntity? = dao.byId(id)

    suspend fun count(): Int = dao.count()

    suspend fun clear() = dao.clear()

    suspend fun isPopulated(): Boolean = count() > 0

    private suspend fun getUpdatedAt(): String? = settings.scryfallUpdatedAt()

    private suspend fun setUpdatedAt(value: String) = settings.setScryfallUpdatedAt(value)

    private suspend fun getLastCheck(): Long = settings.lastScryfallCheck()

    private suspend fun setLastCheck(value: Long) = settings.setLastScryfallCheck(value)

    /** Imports a Scryfall "Default Cards" bulk stream (array or jsonl, gzip ok). */
    suspend fun importBulk(input: InputStream): Int =
        withContext(Dispatchers.IO) {
            ScryfallBulkImport.import(input) { dao.insertAll(it) }
        }

    /** Imports a previously downloaded bulk file. */
    suspend fun importFile(file: File): Int = withContext(Dispatchers.IO) {
        file.inputStream().use { importBulk(it) }
    }

    sealed interface SyncStatus {
        data object Skipped : SyncStatus
        data object UpToDate : SyncStatus
        data class Populated(val inserted: Int) : SyncStatus
        data class Updated(val inserted: Int) : SyncStatus
        data class Error(val message: String) : SyncStatus
    }

    /**
     * Ensures the reference table matches the latest Scryfall Default Cards data.
     * - If the table is empty, downloads and populates it.
     * - Otherwise, every 15 days, checks whether Scryfall published an update and
     *   re-imports when [BulkDataMeta.updatedAt] changed.
     * The downloaded jsonl.gz is always deleted afterwards.
     *
     * ponytail: the 15-day check runs on app launch; use WorkManager for
     * scheduling when the app is closed (no new dependency was added here).
     */
    suspend fun syncIfNeeded(context: Context): SyncStatus {
        val now = System.currentTimeMillis()
        val populated = isPopulated()
        val fifteenDaysMs = 15L * 24 * 60 * 60 * 1000
        if (populated && now - getLastCheck() < fifteenDaysMs) {
            return SyncStatus.Skipped
        }
        val file = File(context.cacheDir, "scryfall-default-cards.jsonl.gz")
        return try {
            val meta = ScryfallBulkClient.getDefaultCardsMeta()
            val stored = getUpdatedAt()
            if (populated && stored == meta.updatedAt) {
                setLastCheck(now)
                return SyncStatus.UpToDate
            }
            if (populated) clear()
            ScryfallBulkClient.download(meta.downloadUri, file)
            val inserted = importFile(file)
            setUpdatedAt(meta.updatedAt)
            setLastCheck(now)
            if (populated) SyncStatus.Updated(inserted) else SyncStatus.Populated(inserted)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncStatus.Error(e.message ?: "Scryfall sync failed")
        } finally {
            file.delete()
        }
    }

    companion object {
        fun create(context: Context): ScryfallRepository {
            val db = AppDatabaseProvider.get(context)
            return ScryfallRepository(db.scryfallCardDao(), SettingsStore(context))
        }
    }
}
