package com.example.scrollcompanion.service.capture

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Finds the app the user was in just before they tapped the bubble.
 * Needs "Usage access" (a special permission the user grants in system settings).
 */
object ForegroundAppDetector {

    private const val LOOKBACK_MS = 60 * 60 * 1000L
    private val IGNORED_PACKAGES = setOf("com.android.systemui")

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** Display name of the last app (other than ours) that came to the foreground, or null. */
    fun lastForegroundAppLabel(context: Context): String? {
        if (!hasUsageAccess(context)) return null
        val packageName = lastForegroundPackage(context) ?: return null
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun lastForegroundPackage(context: Context): String? {
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                pkg != context.packageName &&
                pkg !in IGNORED_PACKAGES
            ) {
                last = pkg
            }
        }
        return last
    }
}
