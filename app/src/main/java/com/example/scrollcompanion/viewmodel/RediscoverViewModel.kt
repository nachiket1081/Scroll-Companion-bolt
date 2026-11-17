package com.example.scrollcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import com.example.scrollcompanion.domain.ResurfacePlanner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RediscoverUiState(
    val items: List<SavedContentEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class RediscoverViewModel(private val repository: ContentRepository) : ViewModel() {

    val uiState: StateFlow<RediscoverUiState> = repository.observeAllContent()
        .map { items ->
            RediscoverUiState(
                items = ResurfacePlanner.due(items, System.currentTimeMillis(), ResurfacePlanner.SCREEN_ITEMS),
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RediscoverUiState())

    fun markReviewed(id: Long) {
        viewModelScope.launch { repository.markReviewed(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
