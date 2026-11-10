package com.example.scrollcompanion.data.local.database

import androidx.room.TypeConverter
import org.json.JSONArray

class StringListConverter {

    @TypeConverter
    fun fromList(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun toList(json: String): List<String> {
        val array = JSONArray(json)
        return List(array.length()) { index -> array.getString(index) }
    }
}
