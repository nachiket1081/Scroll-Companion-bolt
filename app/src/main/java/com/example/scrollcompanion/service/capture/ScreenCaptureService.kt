package com.example.scrollcompanion.service.capture

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.scrollcompanion.service.overlay.FloatingAssistantService
import com.example.scrollcompanion.ui.screens.extractedtext.ExtractedTextActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ScreenCaptureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var frameHandled = false
    private var finished = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            finish(screenshotPath = null)
        }
    }

    private val captureTimeout = Runnable { finish(screenshotPath = null) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_PROJECTION_DATA, Intent::class.java) }
        if (resultCode != Activity.RESULT_OK || data == null) {
            finish(screenshotPath = null)
        } else {
            startCapture(resultCode, data)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        releaseCapture()
        if (!finished) FloatingAssistantService.setBubbleVisible(this, true)
        scope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Screen capture", NotificationManager.IMPORTANCE_LOW),
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Reading this screen")
            .setContentText("Scroll Companion is capturing this screen once to read its text.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        FloatingAssistantService.setBubbleVisible(this, false)
        try {
            val mediaProjection = getSystemService(MediaProjectionManager::class.java)
                .getMediaProjection(resultCode, data)
            projection = mediaProjection
            mediaProjection.registerCallback(projectionCallback, mainHandler)
            val metrics = resources.displayMetrics
            imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
                .also { it.setOnImageAvailableListener({ reader -> onFrame(reader) }, mainHandler) }
        } catch (e: Exception) {
            finish(screenshotPath = null)
            return
        }
        mainHandler.postDelayed({ startMirroring() }, BUBBLE_SETTLE_MS)
    }

    private fun startMirroring() {
        val mediaProjection = projection ?: return
        val reader = imageReader ?: return
        val metrics = resources.displayMetrics
        try {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScrollCompanionCapture",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler,
            )
        } catch (e: Exception) {
            finish(screenshotPath = null)
            return
        }
        mainHandler.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)
    }

    private fun onFrame(reader: ImageReader) {
        if (frameHandled || finished) return
        val image = reader.acquireLatestImage() ?: return
        frameHandled = true
        val bitmap = try {
            image.toBitmap()
        } finally {
            image.close()
        }
        mainHandler.removeCallbacks(captureTimeout)
        scope.launch {
            val path = withContext(Dispatchers.IO) { saveScreenshot(this@ScreenCaptureService, bitmap) }
            bitmap.recycle()
            finish(screenshotPath = path)
        }
    }

    private fun finish(screenshotPath: String?) {
        if (finished) return
        finished = true
        mainHandler.removeCallbacks(captureTimeout)
        releaseCapture()
        FloatingAssistantService.setBubbleVisible(this, true)
        startActivity(ExtractedTextActivity.intent(this, screenshotPath))
        stopSelf()
    }

    private fun releaseCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_PROJECTION_DATA = "projection_data"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1002
        private const val BUBBLE_SETTLE_MS = 200L
        private const val CAPTURE_TIMEOUT_MS = 5_000L
        private const val SCREENSHOT_DIRECTORY = "captures"

        fun start(context: Context, resultCode: Int, data: Intent) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenCaptureService::class.java)
                    .putExtra(EXTRA_RESULT_CODE, resultCode)
                    .putExtra(EXTRA_PROJECTION_DATA, data),
            )
        }

        private fun saveScreenshot(context: Context, bitmap: Bitmap): String {
            val directory = File(context.filesDir, SCREENSHOT_DIRECTORY).apply { mkdirs() }
            val file = File(directory, "capture_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            return file.absolutePath
        }

        private fun Image.toBitmap(): Bitmap {
            val plane = planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * width
            val padded = Bitmap.createBitmap(
                width + rowPadding / plane.pixelStride,
                height,
                Bitmap.Config.ARGB_8888,
            )
            padded.copyPixelsFromBuffer(plane.buffer)
            return Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
        }
    }
}
