package com.example.scrollcompanion.data.repository

import com.example.scrollcompanion.data.local.dao.CategoryDao
import com.example.scrollcompanion.data.local.dao.SavedContentDao
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class ContentRepository(
    private val contentDao: SavedContentDao,
    private val categoryDao: CategoryDao,
    private val screenshotDirectory: File,
) {
    fun observeAllContent(): Flow<List<SavedContentEntity>> = contentDao.observeAll()

    fun observeRecentContent(limit: Int): Flow<List<SavedContentEntity>> =
        contentDao.observeRecent(limit)

    fun observeContentCount(): Flow<Int> = contentDao.observeCount()

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeCategoryCount(): Flow<Int> = categoryDao.observeCount()

    /** Tags used so far, most used first, to offer as one-tap suggestions. */
    fun observeTagSuggestions(limit: Int = 12): Flow<List<String>> =
        contentDao.observeAll().map { items ->
            items.flatMap { it.tags }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        }

    /** Creates a category, or returns the id of the existing one with the same name. */
    suspend fun createCategory(name: String): Long? {
        val clean = name.trim().take(CATEGORY_NAME_MAX_LENGTH)
        if (clean.isEmpty()) return null
        categoryDao.getByName(clean)?.let { return it.id }
        return categoryDao.insert(CategoryEntity(name = clean)).takeIf { it > 0 }
    }

    suspend fun renameCategory(id: Long, name: String) {
        val clean = name.trim().take(CATEGORY_NAME_MAX_LENGTH)
        if (clean.isEmpty()) return
        val existing = categoryDao.getByName(clean)
        if (existing != null && existing.id != id) return
        categoryDao.rename(id, clean)
    }

    /** Items in the deleted category simply become uncategorised. */
    suspend fun deleteCategory(id: Long) = categoryDao.delete(id)

    suspend fun getAll(): List<SavedContentEntity> = contentDao.getAll()

    suspend fun markReviewed(id: Long) {
        val item = contentDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        contentDao.update(item.copy(lastReviewedAt = now, reviewCount = item.reviewCount + 1, updatedAt = now))
    }

    suspend fun getContent(id: Long): SavedContentEntity? = contentDao.getById(id)

    suspend fun updateDetails(id: Long, title: String, note: String?, categoryId: Long?, tags: List<String>) {
        val item = contentDao.getById(id) ?: return
        contentDao.update(
            item.copy(
                title = title,
                personalNotes = note,
                categoryId = categoryId,
                tags = tags,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(item: SavedContentEntity) {
        contentDao.delete(item)
        item.screenshotPath?.let { runCatching { File(it).delete() } }
    }

    suspend fun saveExtractedText(
        title: String,
        note: String?,
        categoryId: Long?,
        tags: List<String>,
        text: String,
        sourceApp: String?,
        sourceUrl: String?,
        tempScreenshotPath: String?,
    ): Long {
        val now = System.currentTimeMillis()
        return contentDao.insert(
            SavedContentEntity(
                title = title,
                extractedText = text,
                screenshotPath = tempScreenshotPath?.let(::persistScreenshot),
                sourceUrl = sourceUrl,
                sourceApp = sourceApp,
                categoryId = categoryId,
                tags = tags,
                personalNotes = note,
                isFavourite = false,
                savedAt = now,
                updatedAt = now,
                lastReviewedAt = null,
                reviewCount = 0,
            ),
        )
    }

    /** Copies the temporary capture into permanent storage so it survives the result screen closing. */
    private fun persistScreenshot(tempPath: String): String? = try {
        val source = File(tempPath)
        if (!source.exists()) {
            null
        } else {
            screenshotDirectory.mkdirs()
            val target = File(screenshotDirectory, "saved_${System.currentTimeMillis()}.jpg")
            source.copyTo(target, overwrite = true)
            target.absolutePath
        }
    } catch (e: Exception) {
        null
    }


    private companion object {
        const val CATEGORY_NAME_MAX_LENGTH = 40
    }
}
