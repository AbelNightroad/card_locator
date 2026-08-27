package com.gitlab.abelnightroad.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CardEntity::class, ScryfallCardEntity::class, DeckEntity::class, DeckCardEntity::class, TagEntity::class, ScanSessionEntity::class, ScannedCardEntity::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun scryfallCardDao(): ScryfallCardDao
    abstract fun deckDao(): DeckDao
    abstract fun tagDao(): TagDao
    abstract fun scanSessionDao(): ScanSessionDao

    companion object {
        const val DATABASE_NAME = "card_locator.db"

        val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`tag` TEXT NOT NULL, PRIMARY KEY(`tag`))")
            db.execSQL("INSERT OR IGNORE INTO `tags` (`tag`) SELECT DISTINCT `tag` FROM `cards`")
        }

        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL("ALTER TABLE `deck_cards` ADD COLUMN `slot` TEXT NOT NULL DEFAULT 'mainboard'")
            db.execSQL("ALTER TABLE `deck_cards` ADD COLUMN `color_identity` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `scryfall_cards` ADD COLUMN `color_identity` TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_5_6 = Migration(5, 6) { db ->
            db.execSQL("ALTER TABLE `scryfall_cards` ADD COLUMN `image_url` TEXT DEFAULT NULL")
        }

        val MIGRATION_6_7 = Migration(6, 7) { db ->
            db.execSQL("ALTER TABLE `scryfall_cards` ADD COLUMN `cmc` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `scryfall_cards` ADD COLUMN `legalities` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `scryfall_cards` ADD COLUMN `reserved` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `scryfall_cards` ADD COLUMN `game_changer` INTEGER NOT NULL DEFAULT 0")
        }

        val MIGRATION_7_8 = Migration(7, 8) { db ->
            db.execSQL("ALTER TABLE `deck_cards` ADD COLUMN `condition` TEXT NOT NULL DEFAULT 'NM'")
            db.execSQL("ALTER TABLE `deck_cards` ADD COLUMN `price_usd` REAL NOT NULL DEFAULT 0.0")
        }

        val MIGRATION_8_9 = Migration(8, 9) { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `scan_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scanned_cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `name` TEXT NOT NULL, `setCode` TEXT NOT NULL, `setName` TEXT NOT NULL, `collectorNumber` TEXT NOT NULL, `rarity` TEXT NOT NULL, `manaCost` TEXT NOT NULL, `typeLine` TEXT NOT NULL, `oracleText` TEXT NOT NULL, `colorIdentity` TEXT NOT NULL, `scryfallId` TEXT NOT NULL, `priceUsd` REAL NOT NULL, `language` TEXT NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `scan_sessions`(`id`) ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_scanned_cards_sessionId` ON `scanned_cards` (`sessionId`)")
        }
    }
}