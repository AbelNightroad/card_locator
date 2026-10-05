package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: CardRepository,
    private val settings: SettingsStore
) : ViewModel() {

    val tagCounts = repository.tagCounts
    val multiCopyCards = repository.multiCopyCards

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    private val _multiCopyOnly = MutableStateFlow(false)
    val multiCopyOnly: StateFlow<Boolean> = _multiCopyOnly

    private val _colorFilter = MutableStateFlow<Set<String>>(emptySet())
    val colorFilter: StateFlow<Set<String>> = _colorFilter

    private val _typeFilter = MutableStateFlow<String?>(null)
    val typeFilter: StateFlow<String?> = _typeFilter

    private val _rarityFilter = MutableStateFlow<String?>(null)
    val rarityFilter: StateFlow<String?> = _rarityFilter

    val themeId = settings.themeId
    val darkMode = settings.darkMode
    val scryfallUpdatedAt = settings.scryfallUpdatedAt
    val fontId = settings.fontId
    val hapticFeedback = settings.hapticFeedback

    fun searchFlow(query: String) = repository.searchByName(query)

    fun advancedSearchFlow(query: String, colors: Set<String>, type: String?, rarity: String?) =
        repository.searchAdvanced(query, colors, type, rarity)

    fun setSearch(value: String) {
        _search.value = value
    }

    fun clearSearch() {
        _search.value = ""
        clearFilters()
    }

    fun clearFilters() {
        _colorFilter.value = emptySet()
        _typeFilter.value = null
        _rarityFilter.value = null
    }

    fun toggleMultiCopyOnly() {
        _multiCopyOnly.value = !_multiCopyOnly.value
    }

    fun toggleColorFilter(color: String) {
        _colorFilter.value = if (color in _colorFilter.value) {
            _colorFilter.value - color
        } else {
            _colorFilter.value + color
        }
    }

    fun setTypeFilter(type: String?) {
        _typeFilter.value = if (_typeFilter.value == type) null else type
    }

    fun setRarityFilter(rarity: String?) {
        _rarityFilter.value = if (_rarityFilter.value == rarity) null else rarity
    }

    fun setTheme(id: String) {
        viewModelScope.launch { settings.setTheme(id) }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch { settings.setDarkMode(dark) }
    }

    fun setFont(id: String) {
        viewModelScope.launch { settings.setFont(id) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settings.setHapticFeedback(enabled) }
    }

    fun createTag(name: String) {
        viewModelScope.launch { repository.createTag(name) }
    }
}
