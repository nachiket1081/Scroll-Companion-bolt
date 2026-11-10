package com.example.scrollcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    fun observeCount(): Flow<Int>
}
