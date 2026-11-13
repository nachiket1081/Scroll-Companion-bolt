package com.example.scrollcompanion.ui.screens.extractedtext

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.repository.ContentRepository
import com.example.scrollcompanion.domain.TitleSuggester
import android.util.Log
import com.example.scrollcompanion.ml.ocr.ScreenTextRecognizer
import com.example.scrollcompanion.ml.ocr.SystemBarInsets
import com.example.scrollcompanion.utils.LinkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ExtractedTextUiState {
    data object Loading : ExtractedTextUiState
    data class Error(val message: String) : ExtractedTextUiState
    data object NoText : ExtractedTextUiState
    data class Ready(val text: String) : ExtractedTextUiState
}

enum class SaveState { Idle, Saving, Saved, Failed }

class ExtractedTextViewModel(
    private val screenshotPath: String?,
    val sourceApp: String?,
    private val systemBarInsets: SystemBarInsets,
    private val repository: ContentRepository,
) : ViewModel() {

    private val recognizer = ScreenTextRecognizer()
    private val _uiState = MutableStateFlow<ExtractedTextUiState>(ExtractedTextUiState.Loading)
    val uiState: StateFlow<ExtractedTextUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _categoryId = MutableStateFlow<Long?>(null)
    val categoryId: StateFlow<Long?> = _categoryId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    val tagSuggestions: StateFlow<List<String>> = repository.observeTagSuggestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private var titleEdited = false

    fun onCategorySelected(id: Long?) {
        _categoryId.value = id
        resetSaveState()
    }

    fun onCreateCategory(name: String) {
        viewModelScope.launch {
            repository.createCategory(name)?.let { _categoryId.value = it }
            resetSaveState()
        }
    }

    fun onTagsChanged(tags: List<String>) {
        _tags.value = tags
        resetSaveState()
    }

    init {
        extractText()
    }

    fun onTitleChanged(value: String) {
        titleEdited = true
        _title.value = value
        resetSaveState()
    }

    fun onNoteChanged(value: String) {
        _note.value = value
        resetSaveState()
    }

    private fun resetSaveState() {
        if (_saveState.value != SaveState.Saving) _saveState.value = SaveState.Idle
    }

    fun onTextChanged(text: String) {
        _uiState.value = ExtractedTextUiState.Ready(text)
        resetSaveState()
    }

    fun save() {
        val text = (_uiState.value as? ExtractedTextUiState.Ready)?.text?.takeIf { it.isNotBlank() } ?: return
        if (_saveState.value == SaveState.Saving) return
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                withContext(Dispatchers.IO) {
                    repository.saveExtractedText(
                        title = _title.value.trim().ifBlank { TitleSuggester.suggest(text, sourceApp, System.currentTimeMillis()) },
                        note = _note.value.trim().ifBlank { null },
                        categoryId = _categoryId.value,
                        tags = _tags.value,
                        text = text,
                        sourceApp = sourceApp,
                        sourceUrl = LinkExtractor.firstUrl(text),
                        tempScreenshotPath = screenshotPath,
                    )
                }
                SaveState.Saved
            } catch (e: Exception) {
                SaveState.Failed
            }
        }
    }

    private fun extractText() {
        val path = screenshotPath
        if (path == null) {
            _uiState.value = ExtractedTextUiState.Error(CAPTURE_FAILED_MESSAGE)
            return
        }
        viewModelScope.launch {
            _uiState.value = try {
                val text = readText(path)
                Log.e("ScrollCapture", "OCR returned ${text.length} chars: \"${text.take(80)}\"")
                if (text.isBlank()) {
                    ExtractedTextUiState.NoText
                } else {
                    if (!titleEdited) _title.value = TitleSuggester.suggest(text, sourceApp, System.currentTimeMillis())
                    ExtractedTextUiState.Ready(text)
                }
            } catch (e: Exception) {
                Log.e("ScrollCapture", "readText threw", e)
                ExtractedTextUiState.Error(READ_FAILED_MESSAGE)
            }
        }
    }

    private suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(path) ?: error("Screenshot could not be decoded")
        try {
            recognizer.recognize(bitmap, systemBarInsets)
        } finally {
            bitmap.recycle()
        }
    }

    override fun onCleared() {
        recognizer.close()
        screenshotPath?.let { File(it).delete() }
        super.onCleared()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val CAPTURE_FAILED_MESSAGE = "Could not capture this screen. Go back to the app you were using and try again."
        const val READ_FAILED_MESSAGE = "Something went wrong while reading this screen. Please try again."
    }
}
