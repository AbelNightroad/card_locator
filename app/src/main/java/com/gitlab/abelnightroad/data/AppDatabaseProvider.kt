package com.gitlab.abelnightroad.data

import android.content.Context
import androidx.room.Room
import com.gitlab.abelnightroad.db.AppDatabase

object AppDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9)
                .fallbackToDestructiveMigration(true).build().also { instance = it }
        }
    }
}
