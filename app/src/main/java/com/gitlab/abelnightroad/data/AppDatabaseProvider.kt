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
            ).fallbackToDestructiveMigration(true).build().also { instance = it }
        }
    }
}
