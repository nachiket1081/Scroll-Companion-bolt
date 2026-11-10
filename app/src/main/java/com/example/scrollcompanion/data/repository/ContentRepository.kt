package com.example.scrollcompanion.data.repository

import com.example.scrollcompanion.data.local.dao.CategoryDao
import com.example.scrollcompanion.data.local.dao.SavedContentDao
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import kotlinx.coroutines.flow.Flow

class ContentRepository(
    private val contentDao: SavedContentDao,
    private val categoryDao: CategoryDao,
) {
    fun observeAllContent(): Flow<List<SavedContentEntity>> = contentDao.observeAll()

    fun observeRecentContent(limit: Int): Flow<List<SavedContentEntity>> =
        contentDao.observeRecent(limit)

    fun observeContentCount(): Flow<Int> = contentDao.observeCount()

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeCategoryCount(): Flow<Int> = categoryDao.observeCount()
}
