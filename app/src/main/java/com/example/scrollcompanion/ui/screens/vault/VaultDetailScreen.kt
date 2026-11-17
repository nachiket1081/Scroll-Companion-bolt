package com.example.scrollcompanion.ui.screens.vault

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.ui.components.CategoryChips
import com.example.scrollcompanion.ui.components.TagEditor
import com.example.scrollcompanion.utils.formatSavedDate
import com.example.scrollcompanion.viewmodel.VaultDetailUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VaultDetailScreen(
    uiState: VaultDetailUiState,
    onBack: () -> Unit,
    categories: List<CategoryEntity>,
    tagSuggestions: List<String>,
    onCreateCategory: (name: String, onCreated: (Long) -> Unit) -> Unit,
    onSaveEdits: (title: String, note: String, categoryId: Long?, tags: List<String>) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val item = uiState.item

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (item != null && !editing) {
                IconButton(onClick = { editing = true }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit title and note",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            item == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This item could not be found.", color = MaterialTheme.colorScheme.onBackground)
            }
            else -> DetailContent(
                item = item,
                categories = categories,
                tagSuggestions = tagSuggestions,
                onCreateCategory = onCreateCategory,
                editing = editing,
                onCancelEdit = { editing = false },
                onSaveEdit = { title, note, categoryId, tags ->
                    onSaveEdits(title, note, categoryId, tags)
                    editing = false
                },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this item?") },
            text = { Text("This removes the saved text, screenshot and note. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailContent(
    item: SavedContentEntity,
    categories: List<CategoryEntity>,
    tagSuggestions: List<String>,
    onCreateCategory: (name: String, onCreated: (Long) -> Unit) -> Unit,
    editing: Boolean,
    onCancelEdit: () -> Unit,
    onSaveEdit: (title: String, note: String, categoryId: Long?, tags: List<String>) -> Unit,
) {
    val context = LocalContext.current
    var titleDraft by remember(item.id, editing) { mutableStateOf(item.title) }
    var noteDraft by remember(item.id, editing) { mutableStateOf(item.personalNotes.orEmpty()) }
    var categoryDraft by remember(item.id, editing) { mutableStateOf(item.categoryId) }
    var tagsDraft by remember(item.id, editing) { mutableStateOf(item.tags) }
    val categoryName = categories.firstOrNull { it.id == item.categoryId }?.name
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val label = listOfNotNull(item.sourceApp?.takeIf { it.isNotBlank() }, categoryName).joinToString(" · ")
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (editing) {
            OutlinedTextField(
                value = titleDraft,
                onValueChange = { titleDraft = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                label = { Text("Why are you saving this?") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CategoryChips(
                categories = categories,
                selectedId = categoryDraft,
                onSelect = { categoryDraft = it },
                onCreate = { name -> onCreateCategory(name) { id -> categoryDraft = id } },
            )
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TagEditor(tags = tagsDraft, suggestions = tagSuggestions, onTagsChange = { tagsDraft = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onSaveEdit(titleDraft, noteDraft, categoryDraft, tagsDraft) }, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
                OutlinedButton(onClick = onCancelEdit, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
        } else {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = formatSavedDate(item.savedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!editing && item.tags.isNotEmpty()) {
            Text(
                text = item.tags.joinToString("  ") { "#$it" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (!editing && !item.personalNotes.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Your note",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.personalNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (!item.sourceUrl.isNullOrBlank()) {
            Text(
                text = item.sourceUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl)))
                    }
                },
            )
        }

        item.screenshotPath?.let { path ->
            val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        if (File(path).exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
                    }.getOrNull()
                }
            }
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Saved screenshot",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            SelectionContainer {
                Text(
                    text = item.extractedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
