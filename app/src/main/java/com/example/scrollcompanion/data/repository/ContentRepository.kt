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

    suspend fun deleteAll(items: List<SavedContentEntity>) {
        items.forEach { delete(it) }
    }

    suspend fun setCategoryForAll(ids: List<Long>, categoryId: Long?) {
        ids.forEach { id ->
            val item = contentDao.getById(id) ?: return@forEach
            contentDao.update(item.copy(categoryId = categoryId, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun delete(item: SavedContentEntity) {
        contentDao.delete(item)
        item.screenshotPath?.let { runCatching { File(it).delete() } }
    }

    /**
     * Fills in a description and/or preview image fetched from the item's link — but only the
     * fields that are still empty, so this never overwrites anything the user already has or
     * has edited by hand. Safe to call from a background job any time after the item was saved.
     */
    suspend fun applyFetchedLinkMetadata(id: Long, description: String?, imagePath: String?) {
        val item = contentDao.getById(id) ?: return
        val newDescription = item.linkDescription ?: description?.take(LINK_DESCRIPTION_MAX_LENGTH)
        val newImagePath = item.previewImagePath ?: item.screenshotPath ?: imagePath
        if (newDescription == item.linkDescription && newImagePath == item.previewImagePath) return
        contentDao.update(
            item.copy(
                linkDescription = newDescription,
                previewImagePath = if (item.screenshotPath == null) newImagePath else item.previewImagePath,
            ),
        )
    }

    /** Downloads a preview image to permanent storage. Returns null on any failure. */
    suspend fun downloadPreviewImage(url: String): String? = try {
        val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            instanceFollowRedirects = true
        }
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            null
        } else {
            screenshotDirectory.mkdirs()
            val target = File(screenshotDirectory, "preview_${System.currentTimeMillis()}.jpg")
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output, bufferSize = 8 * 1024) }
            }
            connection.disconnect()
            target.absolutePath
        }
    } catch (e: Exception) {
        null
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
        const val LINK_DESCRIPTION_MAX_LENGTH = 280
    }
}
