package com.example.scrollcompanion.ui.screens.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.ui.components.CategoryChips
import com.example.scrollcompanion.ui.components.ContentCard
import com.example.scrollcompanion.ui.components.EmptyState
import com.example.scrollcompanion.viewmodel.VaultUiState

@Composable
private fun SelectionToolbar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onCategorize: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row {
            IconButton(onClick = onCategorize) {
                Icon(Icons.Filled.Folder, contentDescription = "Move to category")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
            }
        }
    }
}

@Composable
fun VaultScreen(
    uiState: VaultUiState,
    onItemClick: (Long) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleItemSelected: (Long) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onSetCategoryForSelected: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val selectionMode = uiState.selectedIds.isNotEmpty()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    if (selectionMode) {
        SelectionToolbar(
            count = uiState.selectedIds.size,
            onCancel = onClearSelection,
            onDelete = { showDeleteConfirm = true },
            onCategorize = { showCategoryPicker = true },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${uiState.selectedIds.size} item(s)?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleteSelected() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Move to category") },
            text = {
                Column {
                    TextButton(onClick = { showCategoryPicker = false; onSetCategoryForSelected(null) }) {
                        Text("No category")
                    }
                    uiState.categories.forEach { category ->
                        TextButton(
                            onClick = { showCategoryPicker = false; onSetCategoryForSelected(category.id) },
                        ) { Text(category.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCategoryPicker = false }) { Text("Cancel") } },
        )
    }

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
                        onClick = {
                            if (selectionMode) onToggleItemSelected(item.id) else onItemClick(item.id)
                        },
                        onLongClick = { onToggleItemSelected(item.id) },
                        selectable = selectionMode,
                        selected = item.id in uiState.selectedIds,
                    )
                }
            }
        }
    }
}
