package com.example.scrollcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class VaultUiState(
    val items: List<SavedContentEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class VaultViewModel(repository: ContentRepository) : ViewModel() {

    val uiState: StateFlow<VaultUiState> = repository.observeAllContent()
        .map { items -> VaultUiState(items = items, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), VaultUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
