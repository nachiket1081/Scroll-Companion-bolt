package com.example.scrollcompanion

import android.app.Application
import com.example.scrollcompanion.reminders.ReminderPrefs
import com.example.scrollcompanion.reminders.ReminderScheduler

class ScrollCompanionApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (ReminderPrefs.isEnabled(this)) ReminderScheduler.schedule(this, replace = false)
    }
}
