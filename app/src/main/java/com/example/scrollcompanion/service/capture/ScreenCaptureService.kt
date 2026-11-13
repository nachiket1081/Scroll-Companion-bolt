package com.example.scrollcompanion.service.capture

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationManagerCompat
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
import android.util.Log
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

    private val captureTimeout = Runnable {
        Log.e("ScrollCapture", "captureTimeout fired, no frame")
        finish(screenshotPath = null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("ScrollCapture", "onStartCommand")
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

    private fun Bitmap.isBlank(): Boolean {
        val sampleStep = (width * height / 400).coerceAtLeast(1)
        var firstColor: Int? = null
        var index = 0
        while (index < width * height) {
            val x = index % width
            val y = index / width
            val color = getPixel(x, y)
            if (firstColor == null) {
                firstColor = color
            } else if (color != firstColor) {
                return false
            }
            index += sampleStep
        }
        return true
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
        Log.e("ScrollCapture", "startCapture")
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
            Log.e("ScrollCapture", "startCapture failed", e)
            finish(screenshotPath = null)
            return
        }
        mainHandler.postDelayed({ startMirroring() }, BUBBLE_SETTLE_MS)
    }

    private fun startMirroring() {
        Log.e("ScrollCapture", "startMirroring")
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
            Log.e("ScrollCapture", "createVirtualDisplay failed", e)
            finish(screenshotPath = null)
            return
        }
        mainHandler.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)
    }

    private fun onFrame(reader: ImageReader) {
        Log.e("ScrollCapture", "onFrame called, frameHandled=$frameHandled finished=$finished")
        if (frameHandled || finished) return
        val image = reader.acquireLatestImage() ?: run {
            Log.e("ScrollCapture", "acquireLatestImage returned null")
            return
        }
        val bitmap = try {
            image.toBitmap()
        } finally {
            image.close()
        }
        if (bitmap.isBlank()) {
            Log.e("ScrollCapture", "frame was blank, waiting for the next one")
            bitmap.recycle()
            return
        }
        frameHandled = true
        Log.e("ScrollCapture", "usable frame: ${bitmap.width}x${bitmap.height}")
        mainHandler.removeCallbacks(captureTimeout)
        scope.launch {
            val path = withContext(Dispatchers.IO) { saveScreenshot(this@ScreenCaptureService, bitmap) }
            bitmap.recycle()
            finish(screenshotPath = path)
        }
    }

    private fun finish(screenshotPath: String?) {
        Log.e("ScrollCapture", "finish called, path=$screenshotPath")
        if (finished) return
        finished = true
        mainHandler.removeCallbacks(captureTimeout)
        releaseCapture()
        FloatingAssistantService.setBubbleVisible(this, true)

        val intent = ExtractedTextActivity.intent(this, screenshotPath)

        // A direct launch from a background service is silently dropped on many devices
        // (Android's background-activity-launch limits, no exception thrown). Try it as a
        // bonus for the short window right after the user interacted with the consent dialog...
        try {
            startActivity(intent)
            Log.e("ScrollCapture", "direct startActivity succeeded")
        } catch (e: Exception) {
            Log.e("ScrollCapture", "direct startActivity failed/blocked", e)
        }

        // ...but always back it up with a notification. Tapping a notification is a user
        // gesture, so this launch is never blocked.
        postResultNotification(intent)
        stopSelf()
    }

    private fun postResultNotification(resultIntent: Intent) {
        val pending = PendingIntent.getActivity(
            this,
            RESULT_REQUEST_CODE,
            resultIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(RESULT_CHANNEL_ID, "Extracted text ready", NotificationManager.IMPORTANCE_HIGH),
        )
        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Text extracted")
            .setContentText("Tap to view and save it.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(this).apply {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this@ScreenCaptureService,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
            ) {
                notify(RESULT_NOTIFICATION_ID, notification)
            } else {
                Log.e("ScrollCapture", "no POST_NOTIFICATIONS permission, can't show result notification")
            }
        }
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
        private const val RESULT_CHANNEL_ID = "capture_result"
        private const val RESULT_NOTIFICATION_ID = 1003
        private const val RESULT_REQUEST_CODE = 3

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
