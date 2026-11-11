package com.example.scrollcompanion.ml.ocr

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenTextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): String {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return cleanOcrText(result.textBlocks.joinToString(separator = "\n\n") { it.text })
    }

    fun close() {
        recognizer.close()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private fun cleanOcrText(raw: String): String =
        raw.lines()
            .joinToString(separator = "\n") { line -> line.replace(RUN_OF_SPACES, " ").trim() }
            .replace(EXTRA_BLANK_LINES, "\n\n")
            .trim()

    private companion object {
        val RUN_OF_SPACES = Regex("[ \\t]+")
        val EXTRA_BLANK_LINES = Regex("\n{3,}")
    }
}
