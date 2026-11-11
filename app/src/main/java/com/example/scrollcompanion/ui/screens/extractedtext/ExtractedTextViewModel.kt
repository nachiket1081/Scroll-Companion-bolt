package com.example.scrollcompanion.ui.screens.extractedtext

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollcompanion.ml.ocr.ScreenTextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ExtractedTextUiState {
    data object Loading : ExtractedTextUiState
    data class Error(val message: String) : ExtractedTextUiState
    data object NoText : ExtractedTextUiState
    data class Ready(val text: String) : ExtractedTextUiState
}

class ExtractedTextViewModel(private val screenshotPath: String?) : ViewModel() {

    private val recognizer = ScreenTextRecognizer()
    private val _uiState = MutableStateFlow<ExtractedTextUiState>(ExtractedTextUiState.Loading)
    val uiState: StateFlow<ExtractedTextUiState> = _uiState.asStateFlow()

    init {
        extractText()
    }

    fun onTextChanged(text: String) {
        _uiState.value = ExtractedTextUiState.Ready(text)
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
                if (text.isBlank()) ExtractedTextUiState.NoText else ExtractedTextUiState.Ready(text)
            } catch (e: Exception) {
                ExtractedTextUiState.Error(READ_FAILED_MESSAGE)
            }
        }
    }

    private suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(path) ?: error("Screenshot could not be decoded")
        try {
            recognizer.recognize(bitmap)
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
        const val CAPTURE_FAILED_MESSAGE = "Could not capture this screen. Go back to the app you were using and try again."
        const val READ_FAILED_MESSAGE = "Something went wrong while reading this screen. Please try again."
    }
}
