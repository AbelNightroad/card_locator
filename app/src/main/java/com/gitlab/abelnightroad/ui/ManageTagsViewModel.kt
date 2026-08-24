package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.db.TagCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageTagsViewModel(private val repository: CardRepository) : ViewModel() {

    val tags: StateFlow<List<TagCount>> = repository.tagCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTag(name: String) {
        viewModelScope.launch { repository.createTag(name) }
    }

    fun renameTag(oldTag: String, newTag: String) {
        viewModelScope.launch { repository.renameTag(oldTag, newTag) }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch { repository.deleteByTag(tag) }
    }
}
