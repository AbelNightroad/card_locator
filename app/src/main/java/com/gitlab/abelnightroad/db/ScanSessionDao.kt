package com.gitlab.abelnightroad.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {

    @Insert
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Insert
    suspend fun insertCard(card: ScannedCardEntity): Long

    @Query("SELECT * FROM scanned_cards WHERE sessionId = :sessionId")
    fun getCardsBySession(sessionId: Long): Flow<List<ScannedCardEntity>>

    @Query("SELECT * FROM scanned_cards WHERE sessionId = :sessionId")
    suspend fun getCardsBySessionOnce(sessionId: Long): List<ScannedCardEntity>

    @Query("DELETE FROM scanned_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM scan_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>
}
