package fr.antmu.pianopiano.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar
import java.util.concurrent.TimeUnit

object UsageStatsHelper {

    data class AppUsageStats(
        val totalTimeToday: Long,          // en millisecondes
        val launchCountToday: Int,
        val averageSessionTime: Long,      // en millisecondes
        val lastUsedTime: Long             // timestamp
    )

    fun getAppUsageStats(context: Context, packageName: String): AppUsageStats? {
        if (!PermissionHelper.hasUsageStatsPermission(context)) {
            return null
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        // Get stats for today (from midnight to now)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val appStats = usageStatsList.find { it.packageName == packageName } ?: return null

        // Get events to count sessions and calculate average
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var sessionCount = 0

        while (events.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events.getNextEvent(event)

            if (event.packageName == packageName) {
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    sessionCount++
                }
            }
        }

        // Calculate average session time
        val averageSessionTime = if (sessionCount > 0 && appStats.totalTimeInForeground > 0) {
            appStats.totalTimeInForeground / sessionCount
        } else {
            0L
        }

        return AppUsageStats(
            totalTimeToday = appStats.totalTimeInForeground,
            launchCountToday = sessionCount,
            averageSessionTime = averageSessionTime,
            lastUsedTime = appStats.lastTimeUsed
        )
    }

    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes}min ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatShortDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes}min"
            else -> {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                "${seconds}s"
            }
        }
    }
}
