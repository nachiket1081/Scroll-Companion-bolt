package com.example.scrollcompanion.ui.screens.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import android.content.Context
import com.example.scrollcompanion.domain.LinkMetadataWorker
import com.example.scrollcompanion.domain.SharedContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShareSaveViewModel(
    val content: SharedContent?,
    private val repository: ContentRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _title = MutableStateFlow(content?.title.orEmpty())
    val title: StateFlow<String> = _title.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _categoryId = MutableStateFlow<Long?>(null)
    val categoryId: StateFlow<Long?> = _categoryId.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val tagSuggestions: StateFlow<List<String>> = repository.observeTagSuggestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun onTitleChanged(value: String) {
        _title.value = value
    }

    fun onNoteChanged(value: String) {
        _note.value = value
    }

    fun onCategorySelected(id: Long?) {
        _categoryId.value = id
    }

    fun onCreateCategory(name: String) {
        viewModelScope.launch {
            repository.createCategory(name)?.let { _categoryId.value = it }
        }
    }

    fun onTagsChanged(tags: List<String>) {
        _tags.value = tags
    }

    fun save(onSaved: () -> Unit, onFailed: () -> Unit) {
        val shared = content ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val id = repository.saveExtractedText(
                    title = _title.value.trim().ifBlank { shared.title },
                    note = _note.value.trim().ifBlank { null },
                    categoryId = _categoryId.value,
                    tags = _tags.value,
                    text = shared.text,
                    sourceApp = shared.sourceApp,
                    sourceUrl = shared.url,
                    tempScreenshotPath = null,
                )
                LinkMetadataWorker.enqueue(appContext, id, shared.url)
                onSaved()
            } catch (e: Exception) {
                _isSaving.value = false
                onFailed()
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
