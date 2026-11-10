package com.example.scrollcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val totalSaved: Int = 0,
    val categoryCount: Int = 0,
    val recent: List<SavedContentEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(repository: ContentRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeContentCount(),
        repository.observeCategoryCount(),
        repository.observeRecentContent(RECENT_LIMIT),
    ) { total, categories, recent ->
        HomeUiState(
            totalSaved = total,
            categoryCount = categories,
            recent = recent,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    private companion object {
        const val RECENT_LIMIT = 5
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
