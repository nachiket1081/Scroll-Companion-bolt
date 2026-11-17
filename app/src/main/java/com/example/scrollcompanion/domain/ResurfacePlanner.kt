package com.example.scrollcompanion.domain

import com.example.scrollcompanion.data.local.entities.SavedContentEntity

/**
 * Spaced resurfacing: an item comes back 1 day after it was saved, then 3, 7, 14, 30 and
 * 60 days after each time the user marks it reviewed.
 */
object ResurfacePlanner {

    const val NOTIFICATION_ITEMS = 3
    const val SCREEN_ITEMS = 20

    private val INTERVAL_DAYS = longArrayOf(1, 3, 7, 14, 30, 60)
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun dueAt(item: SavedContentEntity): Long {
        val base = item.lastReviewedAt ?: item.savedAt
        val days = INTERVAL_DAYS[item.reviewCount.coerceIn(0, INTERVAL_DAYS.lastIndex)]
        return base + days * DAY_MILLIS
    }

    /** Items whose review time has passed, most overdue first. */
    fun due(items: List<SavedContentEntity>, now: Long, limit: Int): List<SavedContentEntity> =
        items.filter { dueAt(it) <= now }.sortedBy { dueAt(it) }.take(limit)

    /** Used by the "test reminder" button: ignores the schedule, picks the least recently seen items. */
    fun leastRecentlySeen(items: List<SavedContentEntity>, limit: Int): List<SavedContentEntity> =
        items.sortedBy { it.lastReviewedAt ?: it.savedAt }.take(limit)
}
