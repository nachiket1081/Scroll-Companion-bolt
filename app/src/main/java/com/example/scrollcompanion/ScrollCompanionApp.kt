package com.example.scrollcompanion

import android.app.Application

class ScrollCompanionApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
