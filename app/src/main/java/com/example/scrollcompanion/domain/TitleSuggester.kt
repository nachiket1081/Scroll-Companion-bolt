package com.example.scrollcompanion.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Picks a readable title for a capture instead of whatever OCR happened to read first. */
object TitleSuggester {

    private const val MAX_LENGTH = 60
    private const val MIN_WORDS = 4
    private const val MIN_LETTER_RATIO = 0.6

    private val WHITESPACE = Regex("\\s+")

    // Buttons and labels that show up on almost every feed screen.
    private val UI_TEXT = setOf(
        "follow", "following", "like", "likes", "reply", "share", "send", "more", "see more",
        "subscribe", "comment", "comments", "save", "saved", "home", "search", "reels", "explore",
        "menu", "sponsored", "view all comments", "add a comment", "translate", "show more",
    )

    fun suggest(text: String, sourceApp: String?, now: Long): String {
        val line = text.lineSequence().map { it.trim() }.firstOrNull(::looksLikeTitle)
        return if (line != null) shorten(line) else fallback(sourceApp, now)
    }

    private fun looksLikeTitle(line: String): Boolean {
        if (line.lowercase(Locale.ROOT) in UI_TEXT) return false
        if (line.split(WHITESPACE).count { it.isNotBlank() } < MIN_WORDS) return false
        val visible = line.count { !it.isWhitespace() }
        val letters = line.count { it.isLetter() || isCombiningMark(it) }
        return visible > 0 && letters >= visible * MIN_LETTER_RATIO
    }

    // Keeps Devanagari and other scripts that rely on combining marks from looking "non-letter".
    private fun isCombiningMark(char: Char): Boolean {
        val type = Character.getType(char)
        return type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()
    }

    fun shorten(line: String): String {
        if (line.length <= MAX_LENGTH) return line
        val cut = line.lastIndexOf(' ', MAX_LENGTH).takeIf { it > MAX_LENGTH / 2 } ?: MAX_LENGTH
        return line.substring(0, cut).trimEnd(',', ';', ':', '-', ' ') + "…"
    }

    fun fallback(sourceApp: String?, now: Long): String {
        val date = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            .format(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()))
        return "${sourceApp?.takeIf { it.isNotBlank() } ?: "Saved"} · $date"
    }
}
