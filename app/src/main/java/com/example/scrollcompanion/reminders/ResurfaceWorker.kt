package com.example.scrollcompanion.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.scrollcompanion.MainActivity
import com.example.scrollcompanion.ScrollCompanionApp
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import com.example.scrollcompanion.domain.ResurfacePlanner
import com.example.scrollcompanion.ui.navigation.TopLevelDestination

class ResurfaceWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isTest = inputData.getBoolean(KEY_TEST, false)
        val repository = (applicationContext as ScrollCompanionApp).container.contentRepository
        val items = repository.getAll()
        val chosen = if (isTest) {
            ResurfacePlanner.leastRecentlySeen(items, ResurfacePlanner.NOTIFICATION_ITEMS)
        } else {
            ResurfacePlanner.due(items, System.currentTimeMillis(), ResurfacePlanner.NOTIFICATION_ITEMS)
        }
        if (chosen.isNotEmpty()) postNotification(chosen)
        return Result.success()
    }

    private fun postNotification(items: List<SavedContentEntity>) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val openRediscover = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_START_ROUTE, TopLevelDestination.REDISCOVER.route),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val lines = items.map { item ->
            val headline = item.personalNotes?.takeIf { it.isNotBlank() } ?: item.title
            if (item.sourceApp.isNullOrBlank()) headline else "${item.sourceApp} · $headline"
        }
        val title = if (items.size == 1) "1 saved thing to revisit" else "${items.size} saved things to revisit"
        val inbox = NotificationCompat.InboxStyle().also { style -> lines.forEach(style::addLine) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(inbox)
            .setContentIntent(openRediscover)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_TEST = "test"
        private const val CHANNEL_ID = "daily_reminders"
        private const val NOTIFICATION_ID = 2001
        private const val REQUEST_CODE = 2
    }
}
