package com.example.scrollcompanion.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.scrollcompanion.data.local.dao.CategoryDao
import com.example.scrollcompanion.data.local.dao.SavedContentDao
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.data.local.entities.SavedContentEntity

@Database(
    entities = [SavedContentEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedContentDao(): SavedContentDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "scroll_companion.db"

        private val DEFAULT_CATEGORIES = listOf(
            "Technology & AI",
            "Business Ideas",
            "Finance & Investing",
            "Science & General Knowledge",
            "Travel & Places",
            "Fitness & Health",
            "Productivity",
            "Random Discoveries",
        )

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DEFAULT_CATEGORIES.forEach { name ->
                            db.execSQL("INSERT INTO categories (name) VALUES (?)", arrayOf(name))
                        }
                    }
                })
                .build()
    }
}
