package com.aashik.music.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

fun getCurrentTimeWithAmPm(): String {
    return synchronized(timeFormatter) {
        timeFormatter.format(Date(System.currentTimeMillis()))
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val m = if (minutes < 10) "0$minutes" else minutes.toString()
    val s = if (seconds < 10) "0$seconds" else seconds.toString()
    return "$m:$s"
}
