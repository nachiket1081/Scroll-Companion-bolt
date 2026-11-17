package com.example.scrollcompanion.utils

import android.util.Patterns

object LinkExtractor {

    /** Returns the first web link found in [text] (with https:// added if missing), or null. */
    fun firstUrl(text: String): String? {
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            if (start > 0 && text[start - 1] == '@') continue // part of an email address
            val raw = matcher.group().trimEnd('.', ',', ';', ':', ')', ']')
            if (!raw.contains('.')) continue
            return if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        }
        return null
    }
}
