package com.example.scrollcompanion.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_content",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["savedAt"])],
)
data class SavedContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val extractedText: String,
    val screenshotPath: String?,
    val sourceUrl: String?,
    val sourceApp: String?,
    val categoryId: Long?,
    val tags: List<String>,
    val personalNotes: String?,
    val isFavourite: Boolean,
    val savedAt: Long,
    val updatedAt: Long,
    val lastReviewedAt: Long?,
    val reviewCount: Int,
)
