package com.example.scrollcompanion.domain

import java.util.Locale

object TagNormalizer {
    const val MAX_TAGS = 10
    private const val MAX_LENGTH = 24
    private val WHITESPACE = Regex("\\s+")

    /** "  #Pune Trip " becomes "pune-trip". Returns null if nothing usable is left. */
    fun normalize(raw: String): String? =
        raw.trim()
            .trimStart('#')
            .trim()
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE, "-")
            .take(MAX_LENGTH)
            .trim('-')
            .ifEmpty { null }

    /** Adds one or more comma-separated tags to [tags], skipping duplicates and respecting the limit. */
    fun add(tags: List<String>, raw: String): List<String> {
        var result = tags
        for (part in raw.split(',')) {
            val tag = normalize(part) ?: continue
            if (tag !in result && result.size < MAX_TAGS) result = result + tag
        }
        return result
    }
}
