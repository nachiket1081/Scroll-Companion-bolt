package com.example.scrollcompanion.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.data.local.entities.CategoryEntity

/**
 * A scrollable row of category chips. Tapping the selected chip clears the selection.
 * With [showAll] a leading "All" chip is shown, used for filtering a list.
 * With [onCreate] a trailing "+ New" chip lets the user add their own category.
 */
@Composable
fun CategoryChips(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = false,
    onCreate: ((String) -> Unit)? = null,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showAll) {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
            )
        }
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onSelect(if (category.id == selectedId) null else category.id) },
                label = { Text(category.name) },
            )
        }
        if (onCreate != null) {
            AssistChip(onClick = { showCreateDialog = true }, label = { Text("+ New") })
        }
    }

    if (showCreateDialog && onCreate != null) {
        CategoryNameDialog(
            title = "New category",
            confirmLabel = "Add",
            existingNames = categories.map { it.name },
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreate(name)
            },
        )
    }
}

/** Asks for a category name. Rejects names that already exist (ignoring case). */
@Composable
fun CategoryNameDialog(
    title: String,
    confirmLabel: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialName: String = "",
) {
    var name by remember { mutableStateOf(initialName) }
    val clean = name.trim()
    val duplicate = clean.isNotEmpty() &&
        !clean.equals(initialName.trim(), ignoreCase = true) &&
        existingNames.any { it.equals(clean, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                label = { Text("Name") },
                singleLine = true,
                isError = duplicate,
                supportingText = if (duplicate) ({ Text("That category already exists") }) else null,
            )
        },
        confirmButton = {
            TextButton(enabled = clean.isNotEmpty() && !duplicate, onClick = { onConfirm(clean) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val MAX_NAME_LENGTH = 40
