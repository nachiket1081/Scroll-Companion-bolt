package com.example.scrollcompanion.service.capture

/**
 * Hands the name of the app the user was in (read when they tapped the bubble) over to the result
 * screen, which opens a few seconds later after the capture finishes. Both run in this app's process.
 */
object CaptureSession {
    @Volatile
    var sourceApp: String? = null

    /** Returns the stored name once and clears it, so it can't leak into a later capture. */
    fun take(): String? = sourceApp.also { sourceApp = null }
}
