package com.github.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.abelnightroad.data.CardRepository
import com.github.abelnightroad.data.SettingsStore
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

    val themeId = settings.themeId
    val darkMode = settings.darkMode

    fun searchFlow(query: String) = repository.searchByName(query)

    fun setSearch(value: String) {
        _search.value = value
    }

    fun toggleMultiCopyOnly() {
        _multiCopyOnly.value = !_multiCopyOnly.value
    }

    fun setTheme(id: String) {
        viewModelScope.launch { settings.setTheme(id) }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch { settings.setDarkMode(dark) }
    }
}
