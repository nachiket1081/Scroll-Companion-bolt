package com.example.scrollcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedContentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: SavedContentEntity): Long

    @Update
    suspend fun update(item: SavedContentEntity)

    @Delete
    suspend fun delete(item: SavedContentEntity)

    @Query("SELECT * FROM saved_content WHERE id = :id")
    suspend fun getById(id: Long): SavedContentEntity?

    @Query("SELECT * FROM saved_content")
    suspend fun getAll(): List<SavedContentEntity>

    @Query("SELECT * FROM saved_content ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedContentEntity>>

    @Query("SELECT * FROM saved_content ORDER BY savedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SavedContentEntity>>

    @Query("SELECT COUNT(*) FROM saved_content")
    fun observeCount(): Flow<Int>
}
