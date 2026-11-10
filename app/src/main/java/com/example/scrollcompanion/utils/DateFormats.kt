package com.example.scrollcompanion.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val savedDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

fun formatSavedDate(epochMillis: Long): String =
    savedDateFormatter.format(Instant.ofEpochMilli(epochMillis))
