package com.example.scrollcompanion.ui.screens.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.ui.components.CategoryChips
import com.example.scrollcompanion.ui.components.ContentCard
import com.example.scrollcompanion.ui.components.EmptyState
import com.example.scrollcompanion.viewmodel.VaultUiState

@Composable
fun VaultScreen(
    uiState: VaultUiState,
    onItemClick: (Long) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            uiState.totalCount == 0 -> EmptyState(
                title = "Your vault is empty",
                message = "Saved discoveries will be collected here.",
                modifier = Modifier.align(Alignment.Center),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Knowledge Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                item(key = "search") {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChanged,
                        placeholder = { Text("Search title, text, notes, tags") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChanged("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    CategoryChips(
                        categories = uiState.categories,
                        selectedId = uiState.selectedCategoryId,
                        onSelect = onCategorySelected,
                        showAll = true,
                    )
                }
                if (uiState.items.isEmpty()) {
                    item {
                        Text(
                            text = if (uiState.query.isNotBlank()) {
                                "No matches for \"${uiState.query.trim()}\"."
                            } else {
                                "Nothing saved in this category yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(uiState.items, key = { it.id }) { item ->
                    ContentCard(
                        item = item,
                        categoryName = uiState.categories.firstOrNull { it.id == item.categoryId }?.name,
                        onClick = { onItemClick(item.id) },
                    )
                }
            }
        }
    }
}
