package com.github.abelnightroad.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CardEntity::class, ScryfallCardEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun scryfallCardDao(): ScryfallCardDao

    companion object {
        const val DATABASE_NAME = "card_locator.db"
    }
}
