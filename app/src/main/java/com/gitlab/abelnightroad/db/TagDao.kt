package com.gitlab.abelnightroad.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity)

    @Query("DELETE FROM tags WHERE tag = :tag")
    suspend fun delete(tag: String)

    @Query("DELETE FROM cards WHERE tag = :tag")
    suspend fun deleteCardsByTag(tag: String)

    @Query("UPDATE tags SET tag = :newTag WHERE tag = :oldTag")
    suspend fun rename(oldTag: String, newTag: String)
}
