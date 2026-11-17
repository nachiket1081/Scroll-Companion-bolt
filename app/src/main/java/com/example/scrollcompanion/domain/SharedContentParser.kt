package com.example.scrollcompanion.domain

import android.net.Uri
import com.example.scrollcompanion.utils.LinkExtractor
import java.util.Locale

data class SharedContent(
    val title: String,
    val text: String,
    val url: String?,
    val sourceApp: String?,
)

/** Turns what another app puts in a share intent into something we can save. */
object SharedContentParser {

    private val URL_REGEX = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

    private val KNOWN_SITES = listOf(
        "youtube.com" to "YouTube",
        "youtu.be" to "YouTube",
        "instagram.com" to "Instagram",
        "x.com" to "X",
        "twitter.com" to "X",
        "t.co" to "X",
        "reddit.com" to "Reddit",
        "redd.it" to "Reddit",
        "linkedin.com" to "LinkedIn",
        "facebook.com" to "Facebook",
        "fb.watch" to "Facebook",
        "threads.net" to "Threads",
        "medium.com" to "Medium",
        "github.com" to "GitHub",
        "wikipedia.org" to "Wikipedia",
        "pinterest.com" to "Pinterest",
        "quora.com" to "Quora",
    )

    fun parse(subject: String?, text: String?, now: Long): SharedContent? {
        val rawText = text?.trim().orEmpty()
        val cleanSubject = subject?.trim()?.takeIf { it.isNotEmpty() }
        if (rawText.isEmpty() && cleanSubject == null) return null

        val urlMatch = URL_REGEX.find(rawText)
        val url = urlMatch?.value?.trimEnd('.', ',', ';', ')', ']') ?: LinkExtractor.firstUrl(rawText)
        val body = if (urlMatch != null) rawText.removeRange(urlMatch.range).trim() else rawText
        val sourceApp = url?.let(::siteName)

        val title = cleanSubject
            ?: body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }?.let(TitleSuggester::shorten)
            ?: TitleSuggester.fallback(sourceApp, now)

        return SharedContent(
            title = title,
            text = body.ifBlank { url ?: cleanSubject.orEmpty() },
            url = url,
            sourceApp = sourceApp,
        )
    }

    private fun siteName(url: String): String? {
        val host = runCatching { Uri.parse(url).host }.getOrNull()
            ?.lowercase(Locale.ROOT)
            ?.removePrefix("www.")
            ?.removePrefix("m.")
            ?: return null
        return KNOWN_SITES.firstOrNull { (domain, _) -> host == domain || host.endsWith(".$domain") }?.second ?: host
    }
}
