package com.example.scrollcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultDetailUiState(
    val item: SavedContentEntity? = null,
    val isLoading: Boolean = true,
    val isPendingDelete: Boolean = false,
)

class VaultDetailViewModel(
    private val repository: ContentRepository,
    private val id: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultDetailUiState())
    val uiState: StateFlow<VaultDetailUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    init {
        reload()
    }

    val tagSuggestions: StateFlow<List<String>> = repository.observeTagSuggestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun createCategory(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            repository.createCategory(name)?.let(onCreated)
        }
    }

    fun saveEdits(title: String, note: String, categoryId: Long?, tags: List<String>) {
        val current = _uiState.value.item ?: return
        viewModelScope.launch {
            repository.updateDetails(
                id = id,
                title = title.trim().ifBlank { current.title },
                note = note.trim().ifBlank { null },
                categoryId = categoryId,
                tags = tags,
            )
            reload()
        }
    }

    private var pendingDeleteJob: Job? = null

    /**
     * Marks the item as deleted right away in the UI and starts a short grace period before the
     * row is actually removed from the database, so the user has a moment to tap Undo.
     */
    fun requestDelete(onDeleted: () -> Unit) {
        val current = _uiState.value.item ?: return
        _uiState.update { it.copy(isPendingDelete = true) }
        pendingDeleteJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            repository.delete(current)
            onDeleted()
        }
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _uiState.update { it.copy(isPendingDelete = false) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val UNDO_WINDOW_MS = 4_000L
    }

    private fun reload() {
        viewModelScope.launch {
            _uiState.value = VaultDetailUiState(item = repository.getContent(id), isLoading = false)
        }
    }
}
