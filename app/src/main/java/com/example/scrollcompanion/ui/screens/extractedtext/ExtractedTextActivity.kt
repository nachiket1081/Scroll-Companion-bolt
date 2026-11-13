package com.example.scrollcompanion.ui.screens.extractedtext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scrollcompanion.ScrollCompanionApp
import com.example.scrollcompanion.ml.ocr.SystemBarInsets
import com.example.scrollcompanion.service.capture.CaptureSession
import com.example.scrollcompanion.ui.theme.ScrollCompanionTheme

class ExtractedTextActivity : ComponentActivity() {

    private val viewModel: ExtractedTextViewModel by viewModels {
        viewModelFactory {
            initializer {
                ExtractedTextViewModel(
                    screenshotPath = intent.getStringExtra(EXTRA_SCREENSHOT_PATH),
                    sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP) ?: CaptureSession.take(),
                    systemBarInsets = SystemBarInsets.from(applicationContext),
                    repository = (application as ScrollCompanionApp).container.contentRepository,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("ScrollCapture", "ExtractedTextActivity.onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val saveState by viewModel.saveState.collectAsStateWithLifecycle()
            val title by viewModel.title.collectAsStateWithLifecycle()
            val note by viewModel.note.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            val categoryId by viewModel.categoryId.collectAsStateWithLifecycle()
            val tags by viewModel.tags.collectAsStateWithLifecycle()
            val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
            ScrollCompanionTheme {
                ExtractedTextScreen(
                    uiState = uiState,
                    saveState = saveState,
                    sourceApp = viewModel.sourceApp,
                    title = title,
                    note = note,
                    onTitleChanged = viewModel::onTitleChanged,
                    onNoteChanged = viewModel::onNoteChanged,
                    categories = categories,
                    selectedCategoryId = categoryId,
                    onCategorySelected = viewModel::onCategorySelected,
                    onCreateCategory = viewModel::onCreateCategory,
                    tags = tags,
                    tagSuggestions = tagSuggestions,
                    onTagsChanged = viewModel::onTagsChanged,
                    onTextChanged = viewModel::onTextChanged,
                    onSave = viewModel::save,
                    onClose = { finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
        private const val EXTRA_SOURCE_APP = "source_app"

        fun intent(context: Context, screenshotPath: String?, sourceApp: String? = null): Intent =
            Intent(context, ExtractedTextActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_SCREENSHOT_PATH, screenshotPath)
                .putExtra(EXTRA_SOURCE_APP, sourceApp)
    }
}
