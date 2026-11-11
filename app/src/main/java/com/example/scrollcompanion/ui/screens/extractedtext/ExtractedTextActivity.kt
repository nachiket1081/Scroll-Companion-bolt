package com.example.scrollcompanion.ui.screens.extractedtext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scrollcompanion.ui.theme.ScrollCompanionTheme

class ExtractedTextActivity : ComponentActivity() {

    private val viewModel: ExtractedTextViewModel by viewModels {
        viewModelFactory {
            initializer { ExtractedTextViewModel(intent.getStringExtra(EXTRA_SCREENSHOT_PATH)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ScrollCompanionTheme {
                ExtractedTextScreen(
                    uiState = uiState,
                    onTextChanged = viewModel::onTextChanged,
                    onClose = { finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SCREENSHOT_PATH = "screenshot_path"

        fun intent(context: Context, screenshotPath: String?): Intent =
            Intent(context, ExtractedTextActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_SCREENSHOT_PATH, screenshotPath)
    }
}
