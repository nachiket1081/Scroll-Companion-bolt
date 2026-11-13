package com.example.scrollcompanion.ml.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.min

class ScreenTextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Reads the text in [bitmap]. The status bar and navigation bar are cut off first so the clock,
     * battery and notification icons don't end up in the result.
     */
    suspend fun recognize(bitmap: Bitmap, insets: SystemBarInsets = SystemBarInsets.NONE): String {
        val top = insets.top.coerceIn(0, bitmap.height / MAX_CROP_DIVISOR)
        val bottom = insets.bottom.coerceIn(0, bitmap.height / MAX_CROP_DIVISOR)
        val cropped = if (top + bottom > 0) {
            Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bitmap.height - top - bottom)
        } else {
            bitmap
        }
        try {
            val result = recognizer.process(InputImage.fromBitmap(cropped, 0)).await()
            return cleanOcrText(readingOrder(result))
        } finally {
            if (cropped !== bitmap) cropped.recycle()
        }
    }

    fun close() {
        recognizer.close()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private class OcrLine(val text: String, val rect: Rect)

    /**
     * ML Kit's block order can jump around on busy screens. Instead, take every line, sort top to
     * bottom, put lines that sit on the same row left to right, and add a blank line wherever
     * there is a clear vertical gap.
     */
    private fun readingOrder(result: Text): String {
        val lines = result.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line -> line.boundingBox?.let { OcrLine(line.text, it) } }
            .sortedBy { it.rect.top }

        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines) {
            val row = rows.lastOrNull()
            if (row != null && onSameRow(row.first(), line)) row.add(line) else rows.add(mutableListOf(line))
        }

        val builder = StringBuilder()
        var previousBottom = 0
        var previousHeight = 0
        rows.forEachIndexed { index, row ->
            val top = row.minOf { it.rect.top }
            val bottom = row.maxOf { it.rect.bottom }
            if (index > 0) {
                val gap = top - previousBottom
                builder.append(if (gap > previousHeight * PARAGRAPH_GAP_RATIO) "\n\n" else "\n")
            }
            builder.append(row.sortedBy { it.rect.left }.joinToString(" ") { it.text.replace('\n', ' ') })
            previousBottom = bottom
            previousHeight = bottom - top
        }
        return builder.toString()
    }

    private fun onSameRow(a: OcrLine, b: OcrLine): Boolean {
        val centerDistance = abs(a.rect.exactCenterY() - b.rect.exactCenterY())
        return centerDistance < SAME_ROW_RATIO * min(a.rect.height(), b.rect.height())
    }

    private fun cleanOcrText(raw: String): String =
        raw.lines()
            .joinToString(separator = "\n") { line -> line.replace(RUN_OF_SPACES, " ").trim() }
            .replace(EXTRA_BLANK_LINES, "\n\n")
            .trim()

    private companion object {
        const val MAX_CROP_DIVISOR = 4
        const val SAME_ROW_RATIO = 0.5f
        const val PARAGRAPH_GAP_RATIO = 0.8f
        val RUN_OF_SPACES = Regex("[ \\t]+")
        val EXTRA_BLANK_LINES = Regex("\n{3,}")
    }
}
