package com.gitlab.abelnightroad.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_id")
    private val darkKey = booleanPreferencesKey("dark_mode")
    private val scryfallUpdatedAtKey = stringPreferencesKey("scryfall_updated_at")
    private val lastScryfallCheckKey = longPreferencesKey("scryfall_last_check")

    val themeId: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "nord" }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[darkKey] ?: true }
    val scryfallUpdatedAt: Flow<String?> = context.dataStore.data.map { it[scryfallUpdatedAtKey] }

    suspend fun setTheme(id: String) {
        context.dataStore.edit { it[themeKey] = id }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { it[darkKey] = dark }
    }

    suspend fun scryfallUpdatedAt(): String? =
        context.dataStore.data.first()[scryfallUpdatedAtKey]

    suspend fun setScryfallUpdatedAt(value: String) {
        context.dataStore.edit { it[scryfallUpdatedAtKey] = value }
    }

    suspend fun lastScryfallCheck(): Long =
        context.dataStore.data.first()[lastScryfallCheckKey] ?: 0L

    suspend fun setLastScryfallCheck(value: Long) {
        context.dataStore.edit { it[lastScryfallCheckKey] = value }
    }
}
