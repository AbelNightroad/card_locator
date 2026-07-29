package com.gitlab.abelnightroad.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CardEntity::class, ScryfallCardEntity::class, DeckEntity::class, DeckCardEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun scryfallCardDao(): ScryfallCardDao
    abstract fun deckDao(): DeckDao

    companion object {
        const val DATABASE_NAME = "card_locator.db"
    }
}