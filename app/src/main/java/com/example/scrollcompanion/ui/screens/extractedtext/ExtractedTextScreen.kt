package com.example.scrollcompanion.ui.screens.extractedtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.ui.components.CategoryChips
import com.example.scrollcompanion.ui.components.EmptyState
import com.example.scrollcompanion.ui.components.TagEditor
import com.example.scrollcompanion.utils.LinkExtractor

@Composable
fun ExtractedTextScreen(
    uiState: ExtractedTextUiState,
    saveState: SaveState,
    sourceApp: String?,
    title: String,
    note: String,
    onTitleChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onCreateCategory: (String) -> Unit,
    tags: List<String>,
    tagSuggestions: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    onTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
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
            text = "Save to vault",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (sourceApp != null) {
            Text(
                text = "From $sourceApp",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when (uiState) {
            ExtractedTextUiState.Loading -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Reading text on this screen...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ExtractedTextUiState.Error -> EmptyState(
                title = "Couldn't read this screen",
                message = uiState.message,
            )

            ExtractedTextUiState.NoText -> EmptyState(
                title = "No text found",
                message = "This screen didn't contain any readable text.",
            )

            is ExtractedTextUiState.Ready -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChanged,
                    label = { Text("Title") },
                    singleLine = true,
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
                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = onTextChanged,
                    label = { Text("Extracted text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        val readyText = (uiState as? ExtractedTextUiState.Ready)?.text.orEmpty()
        val detectedLink = remember(readyText) { LinkExtractor.firstUrl(readyText) }
        if (detectedLink != null) {
            Text(
                text = "Link found: $detectedLink",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (readyText.isNotBlank()) {
            val clipboard = LocalClipboardManager.current
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSave,
                    enabled = saveState != SaveState.Saving && saveState != SaveState.Saved,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (saveState) {
                            SaveState.Idle -> "Save to vault"
                            SaveState.Saving -> "Saving..."
                            SaveState.Saved -> "Saved"
                            SaveState.Failed -> "Retry save"
                        },
                    )
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(readyText)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Copy")
                }
            }
        }

        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}
