package com.example.scrollcompanion.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily_resurface"

    /**
     * Runs once a day at about the chosen hour (Android may shift it a little to save battery).
     * [replace] = true restarts the schedule, e.g. after the hour changes; false keeps an existing one.
     */
    fun schedule(context: Context, replace: Boolean) {
        val request = PeriodicWorkRequestBuilder<ResurfaceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNext(ReminderPrefs.hour(context)), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun sendTest(context: Context) {
        val request = OneTimeWorkRequestBuilder<ResurfaceWorker>()
            .setInputData(workDataOf(ResurfaceWorker.KEY_TEST to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun delayUntilNext(hour: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
