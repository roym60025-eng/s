package com.example.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayTimeFormat = SimpleDateFormat("EEE, h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    fun formatMessageTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Now"
        return timeFormat.format(date)
    }

    fun formatChatListTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return ""
        val cal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }
        return when {
            cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> timeFormat.format(date)
            cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
            else -> dateFormat.format(date)
        }
    }

    fun formatLastSeen(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "recently"
        val diffMs = System.currentTimeMillis() - date.time
        return when {
            diffMs < 60_000 -> "just now"
            diffMs < 3_600_000 -> "${diffMs / 60_000}m ago"
            diffMs < 86_400_000 -> "today at ${timeFormat.format(date)}"
            else -> dayTimeFormat.format(date)
        }
    }

    fun formatDurationMs(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun formatStatusAge(createdAt: Timestamp?): String {
        val date = createdAt?.toDate() ?: Date()
        val diffMin = ((System.currentTimeMillis() - date.time) / 60_000L).coerceAtLeast(0L)
        return when {
            diffMin < 1L -> "Just now"
            diffMin < 60L -> "${diffMin}m ago"
            else -> "${diffMin / 60L}h ago"
        }
    }
}
