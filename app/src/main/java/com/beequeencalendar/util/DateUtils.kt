package com.beequeencalendar.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
private val DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy (EE)", Locale("ru"))

fun String.toDisplayDate(): String = try {
    LocalDate.parse(this, ISO).format(DISPLAY)
} catch (_: Exception) {
    this
}

fun LocalDate.toIso(): String = format(ISO)
