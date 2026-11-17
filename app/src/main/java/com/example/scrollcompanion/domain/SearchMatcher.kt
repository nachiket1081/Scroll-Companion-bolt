package com.example.scrollcompanion.domain

import com.example.scrollcompanion.data.local.entities.SavedContentEntity
import java.util.Locale

object SearchMatcher {
    private val WHITESPACE = Regex("\\s+")

    /** Every word in [query] must appear somewhere in the item's title, text, note, app, link, tags or category. */
    fun matches(item: SavedContentEntity, categoryName: String?, query: String): Boolean {
        val words = query.trim()
            .lowercase(Locale.ROOT)
            .split(WHITESPACE)
            .map { it.removePrefix("#") }
            .filter { it.isNotEmpty() }
        if (words.isEmpty()) return true

        val haystack = listOfNotNull(
            item.title,
            item.extractedText,
            item.personalNotes,
            item.sourceApp,
            item.sourceUrl,
            item.tags.joinToString(" "),
            categoryName,
        ).joinToString(" ").lowercase(Locale.ROOT)
        return words.all { it in haystack }
    }
}
