package com.example.scrollcompanion.domain

/** The short snippet shown on cards, without repeating the line already used as the title. */
object ContentPreview {

    private const val MAX_LENGTH = 140

    fun of(text: String, title: String): String {
        val titleStart = title.removeSuffix("…").trim()
        var skippedTitle = false
        val rest = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { line ->
                if (!skippedTitle && titleStart.isNotEmpty() && line.startsWith(titleStart)) {
                    skippedTitle = true
                    false
                } else {
                    true
                }
            }
            .joinToString(" ")
        return if (rest.length <= MAX_LENGTH) rest else rest.substring(0, MAX_LENGTH).trimEnd() + "…"
    }
}
