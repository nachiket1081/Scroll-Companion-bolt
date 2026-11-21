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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

data class VaultUiState(
    val selectedIds: Set<Long> = emptySet(),
    val items: List<SavedContentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val query: String = "",
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
)

class VaultViewModel(private val repository: ContentRepository) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<VaultUiState> = combine(
        repository.observeAllContent(),
        repository.observeCategories(),
        selectedCategoryId,
        query,
        selectedIds,
    ) { items, categories, selected, searchText, selection ->
        val categoryNames = categories.associate { it.id to it.name }
        val visible = items.filter { item ->
            (selected == null || item.categoryId == selected) &&
                SearchMatcher.matches(item, item.categoryId?.let(categoryNames::get), searchText)
        }
        VaultUiState(
            selectedIds = selection.intersect(visible.map { it.id }.toSet()),
            items = visible,
            categories = categories,
            selectedCategoryId = selected,
            query = searchText,
            totalCount = items.size,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), VaultUiState())

    fun toggleSelected(id: Long) {
        selectedIds.value = selectedIds.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val items = uiState.value.items.filter { it.id in ids }
            repository.deleteAll(items)
            selectedIds.value = emptySet()
        }
    }

    fun setCategoryForSelected(categoryId: Long?) {
        val ids = selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setCategoryForAll(ids.toList(), categoryId)
            selectedIds.value = emptySet()
        }
    }

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
