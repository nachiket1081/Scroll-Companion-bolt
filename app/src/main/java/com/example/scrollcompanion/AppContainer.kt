package com.example.scrollcompanion

import android.content.Context
import com.example.scrollcompanion.data.local.database.AppDatabase
import com.example.scrollcompanion.data.repository.ContentRepository

class AppContainer(context: Context) {
    private val database: AppDatabase = AppDatabase.build(context)

    val contentRepository: ContentRepository = ContentRepository(
        contentDao = database.savedContentDao(),
        categoryDao = database.categoryDao(),
    )
}
