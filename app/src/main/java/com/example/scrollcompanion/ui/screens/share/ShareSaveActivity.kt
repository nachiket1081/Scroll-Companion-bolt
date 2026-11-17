package com.example.scrollcompanion.ui.screens.share

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Intent
import com.example.scrollcompanion.ScrollCompanionApp
import com.example.scrollcompanion.domain.SharedContentParser
import com.example.scrollcompanion.ui.theme.ScrollCompanionTheme

/** Receives "Share > Scroll Companion" from any app and saves the link, title and text. */
class ShareSaveActivity : ComponentActivity() {

    private val viewModel: ShareSaveViewModel by viewModels {
        viewModelFactory {
            initializer {
                ShareSaveViewModel(
                    content = SharedContentParser.parse(
                        subject = (intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)
                            ?: intent.getCharSequenceExtra(Intent.EXTRA_TITLE))?.toString(),
                        text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
                        now = System.currentTimeMillis(),
                    ),
                    repository = (application as ScrollCompanionApp).container.contentRepository,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = viewModel.content
        if (content == null) {
            Toast.makeText(this, "Nothing to save from this share.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val title by viewModel.title.collectAsStateWithLifecycle()
            val note by viewModel.note.collectAsStateWithLifecycle()
            val categoryId by viewModel.categoryId.collectAsStateWithLifecycle()
            val tags by viewModel.tags.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
            val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
            ScrollCompanionTheme {
                ShareSaveScreen(
                    content = content,
                    title = title,
                    note = note,
                    categories = categories,
                    selectedCategoryId = categoryId,
                    tags = tags,
                    tagSuggestions = tagSuggestions,
                    isSaving = isSaving,
                    onTitleChanged = viewModel::onTitleChanged,
                    onNoteChanged = viewModel::onNoteChanged,
                    onCategorySelected = viewModel::onCategorySelected,
                    onCreateCategory = viewModel::onCreateCategory,
                    onTagsChanged = viewModel::onTagsChanged,
                    onSave = {
                        viewModel.save(
                            onSaved = {
                                Toast.makeText(this, "Saved to Scroll Companion", Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            onFailed = {
                                Toast.makeText(this, "Could not save. Please try again.", Toast.LENGTH_SHORT).show()
                            },
                        )
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}
