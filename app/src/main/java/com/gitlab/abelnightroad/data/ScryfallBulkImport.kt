package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.ScryfallCardEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.io.SequenceInputStream
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets

/**
 * Streams Scryfall's "Default Cards" bulk data into the reference table.
 *
 * Accepts both formats Scryfall publishes, transparently:
 *  - a gzip-compressed JSON array (`download_uri`)
 *  - a gzip-compressed JSON Lines file (`jsonl_download_uri`)
 *
 * Streaming keeps memory flat even for the full ~557 MB file. The temporary
 * download is deleted by the caller once [import] returns.
 */
object ScryfallBulkImport {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun import(
        input: InputStream,
        batchSize: Int = 500,
        insert: suspend (List<ScryfallCardEntity>) -> Unit
    ): Int {
        val raw = maybeGunzip(input)
        val pb = PushbackInputStream(raw, 1)
        val first = pb.read()
        if (first != -1) pb.unread(first)
        return if (first == '['.code) {
            importArray(pb, batchSize, insert)
        } else {
            importJsonLines(BufferedReader(InputStreamReader(pb, Charsets.UTF_8)), batchSize, insert)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun importArray(
        stream: InputStream,
        batchSize: Int,
        insert: suspend (List<ScryfallCardEntity>) -> Unit
    ): Int {
        var total = 0
        json.decodeToSequence<ScryfallBulkCard>(stream)
            .mapNotNull { it.toEntity() }
            .chunked(batchSize)
            .forEach { batch ->
                insert(batch)
                total += batch.size
            }
        return total
    }

    private suspend fun importJsonLines(
        reader: BufferedReader,
        batchSize: Int,
        insert: suspend (List<ScryfallCardEntity>) -> Unit
    ): Int {
        var total = 0
        val batch = ArrayList<ScryfallCardEntity>(batchSize)
        while (true) {
            val line = reader.readLine() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed == "]") continue
            val card = runCatching { json.decodeFromString<ScryfallBulkCard>(trimmed) }.getOrNull()
                ?: continue
            val entity = card.toEntity() ?: continue
            batch.add(entity)
            if (batch.size >= batchSize) {
                insert(batch)
                total += batch.size
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            insert(batch)
            total += batch.size
        }
        return total
    }

    /** Wraps [input] in a GZIP stream when it begins with the gzip magic bytes. */
    private fun maybeGunzip(input: InputStream): InputStream {
        val head = ByteArray(2)
        val n = runCatching { input.read(head) }.getOrDefault(-1)
        if (n <= 0) return input
        val isGz = head[0] == 0x1f.toByte() && head[1] == 0x8b.toByte()
        val prefix = ByteArrayInputStream(head.copyOf(n))
        return if (isGz) GZIPInputStream(SequenceInputStream(prefix, input))
        else SequenceInputStream(prefix, input)
    }
}

private fun ScryfallBulkCard.toEntity(): ScryfallCardEntity? {
    if (id.isBlank() || name.isBlank()) return null
    return ScryfallCardEntity(
        id = id,
        name = name,
        setCode = setCode,
        setName = setName,
        collectorNumber = collectorNumber,
        rarity = rarity,
        manaCost = manaCost,
        typeLine = typeLine,
        oracleText = oracleText
    )
}
