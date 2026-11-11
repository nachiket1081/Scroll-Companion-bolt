package com.example.scrollcompanion.service.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.scrollcompanion.MainActivity
import com.example.scrollcompanion.service.capture.CaptureConsentActivity
import com.example.scrollcompanion.ui.navigation.TopLevelDestination
import com.example.scrollcompanion.ui.theme.ScrollCompanionTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class FloatingAssistantService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayLifecycle: OverlayLifecycleOwner? = null
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
    private var expanded by mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        startAsForeground()
        if (!canDrawOverlays(this)) {
            stopSelf()
            return
        }
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SHOW_BUBBLE -> overlayView?.visibility = View.VISIBLE
            ACTION_HIDE_BUBBLE -> overlayView?.visibility = View.INVISIBLE
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        overlayLifecycle?.destroy()
        overlayLifecycle = null
        _isRunning.value = false
        super.onDestroy()
    }

    private fun startAsForeground() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Floating assistant", NotificationManager.IMPORTANCE_LOW),
        )
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingAssistantService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Floating assistant is on")
            .setContentText("Tap to open Scroll Companion.")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun addOverlay() {
        val lifecycle = OverlayLifecycleOwner().also { it.start() }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            setContent {
                ScrollCompanionTheme {
                    FloatingAssistantOverlay(
                        expanded = expanded,
                        onBubbleTap = { expanded = !expanded },
                        onDrag = { dx, dy -> moveBy(dx, dy) },
                        onCollapse = { expanded = false },
                        onExtractText = ::requestScreenText,
                        onOpenVault = ::openVault,
                        onHide = { stopSelf() },
                    )
                }
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> keepOnScreen() }
        }

        layoutParams.x = resources.displayMetrics.widthPixels
        layoutParams.y = resources.displayMetrics.heightPixels / 3
        overlayLifecycle = lifecycle
        overlayView = view
        windowManager.addView(view, layoutParams)
        _isRunning.value = true
    }

    private fun moveBy(dx: Float, dy: Float) {
        layoutParams.x += dx.roundToInt()
        layoutParams.y += dy.roundToInt()
        keepOnScreen()
    }

    private fun keepOnScreen() {
        val view = overlayView ?: return
        val metrics = resources.displayMetrics
        layoutParams.x = layoutParams.x.coerceIn(0, (metrics.widthPixels - view.width).coerceAtLeast(0))
        layoutParams.y = layoutParams.y.coerceIn(0, (metrics.heightPixels - view.height).coerceAtLeast(0))
        windowManager.updateViewLayout(view, layoutParams)
    }

    private fun requestScreenText() {
        expanded = false
        startActivity(
            Intent(this, CaptureConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun openVault() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_START_ROUTE, TopLevelDestination.VAULT.route),
        )
        expanded = false
    }

    companion object {
        private const val ACTION_STOP = "com.example.scrollcompanion.action.STOP_FLOATING_ASSISTANT"
        private const val ACTION_SHOW_BUBBLE = "com.example.scrollcompanion.action.SHOW_BUBBLE"
        private const val ACTION_HIDE_BUBBLE = "com.example.scrollcompanion.action.HIDE_BUBBLE"
        private const val CHANNEL_ID = "floating_assistant"
        private const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        fun overlayPermissionIntent(context: Context): Intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, FloatingAssistantService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingAssistantService::class.java))
        }

        fun setBubbleVisible(context: Context, visible: Boolean) {
            context.startService(
                Intent(context, FloatingAssistantService::class.java)
                    .setAction(if (visible) ACTION_SHOW_BUBBLE else ACTION_HIDE_BUBBLE),
            )
        }
    }
}
