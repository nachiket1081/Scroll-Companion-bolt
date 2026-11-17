package com.example.scrollcompanion.reminders

import android.content.Context

object ReminderPrefs {
    private const val FILE = "reminders"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    const val DEFAULT_HOUR = 9

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hour(context: Context): Int = prefs(context).getInt(KEY_HOUR, DEFAULT_HOUR)

    fun setHour(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_HOUR, hour).apply()
    }
}
