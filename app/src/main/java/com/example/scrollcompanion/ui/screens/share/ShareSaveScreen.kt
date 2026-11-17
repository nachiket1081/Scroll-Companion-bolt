package com.example.scrollcompanion.ui.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.domain.SharedContent
import com.example.scrollcompanion.ui.components.CategoryChips
import com.example.scrollcompanion.ui.components.TagEditor

@Composable
fun ShareSaveScreen(
    content: SharedContent,
    title: String,
    note: String,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    tags: List<String>,
    tagSuggestions: List<String>,
    isSaving: Boolean,
    onTitleChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onTagsChanged: (List<String>) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Save to Scroll Companion",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (content.sourceApp != null) {
                Text(
                    text = "From ${content.sourceApp}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (content.url != null) {
                Text(
                    text = content.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChanged,
                label = { Text("Why are you saving this? (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Category (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CategoryChips(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = onCategorySelected,
                onCreate = onCreateCategory,
            )
            Text(
                text = "Tags (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TagEditor(tags = tags, suggestions = tagSuggestions, onTagsChange = onTagsChanged)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                Text(if (isSaving) "Saving..." else "Save")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
    }
}
