package com.example.scrollcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import com.example.scrollcompanion.domain.SearchMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class VaultUiState(
    val items: List<SavedContentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val query: String = "",
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
)

class VaultViewModel(repository: ContentRepository) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<VaultUiState> = combine(
        repository.observeAllContent(),
        repository.observeCategories(),
        selectedCategoryId,
        query,
    ) { items, categories, selected, searchText ->
        val categoryNames = categories.associate { it.id to it.name }
        VaultUiState(
            items = items.filter { item ->
                (selected == null || item.categoryId == selected) &&
                    SearchMatcher.matches(item, item.categoryId?.let(categoryNames::get), searchText)
            },
            categories = categories,
            selectedCategoryId = selected,
            query = searchText,
            totalCount = items.size,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), VaultUiState())

    fun selectCategory(id: Long?) {
        selectedCategoryId.value = id
    }

    fun onQueryChanged(value: String) {
        query.value = value
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
