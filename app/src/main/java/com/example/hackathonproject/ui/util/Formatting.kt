package com.example.hackathonproject.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateTimeFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"))

/** Human-readable date/time for a stored timestamp. */
fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

/** Money with two decimals, locale-independent so the decimal separator stays a dot. */
fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f", value)
