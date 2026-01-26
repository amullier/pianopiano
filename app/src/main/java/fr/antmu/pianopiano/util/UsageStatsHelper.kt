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
        val lastUsedTime: Long,            // timestamp
        val totalTimeLast7Days: Long = 0L, // en millisecondes
        val launchCountLast7Days: Int = 0,
        val totalTimeLast30Days: Long = 0L, // en millisecondes
        val launchCountLast30Days: Int = 0
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
        val startTimeToday = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimeToday,
            endTime
        )

        val appStats = usageStatsList.find { it.packageName == packageName } ?: return null

        // Get events to count sessions and calculate ACTUAL time today from events
        val events = usageStatsManager.queryEvents(startTimeToday, endTime)
        var sessionCount = 0
        var lastForegroundPackage: String? = null
        var totalTimeTodayFromEvents = 0L
        var lastResumeTime: Long? = null

        while (events.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events.getNextEvent(event)

            if (event.packageName == packageName) {
                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (lastForegroundPackage != event.packageName) {
                            sessionCount++
                            lastForegroundPackage = event.packageName
                        }
                        lastResumeTime = event.timeStamp
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                    android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                        lastResumeTime?.let { resumeTime ->
                            totalTimeTodayFromEvents += event.timeStamp - resumeTime
                        }
                        lastResumeTime = null
                    }
                }
            } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                // App switched away, count the time if we were tracking
                lastResumeTime?.let { resumeTime ->
                    totalTimeTodayFromEvents += event.timeStamp - resumeTime
                }
                lastResumeTime = null
                lastForegroundPackage = event.packageName
            }
        }

        // If app is still in foreground, add time until now
        lastResumeTime?.let { resumeTime ->
            totalTimeTodayFromEvents += endTime - resumeTime
        }

        // Calculate average session time
        val averageSessionTime = if (sessionCount > 0 && totalTimeTodayFromEvents > 0) {
            totalTimeTodayFromEvents / sessionCount
        } else {
            0L
        }

        // Get stats for last 7 days
        val startTime7Days = endTime - TimeUnit.DAYS.toMillis(7)
        val usageStatsList7Days = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime7Days,
            endTime
        )

        var totalTime7Days = 0L
        var launchCount7Days = 0

        // Aggregate stats for the last 7 days
        for (stats in usageStatsList7Days) {
            if (stats.packageName == packageName) {
                totalTime7Days += stats.totalTimeInForeground
            }
        }

        // Count launches for last 7 days
        val events7Days = usageStatsManager.queryEvents(startTime7Days, endTime)
        var lastForegroundPackage7Days: String? = null
        while (events7Days.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events7Days.getNextEvent(event)

            if (event.packageName == packageName) {
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (lastForegroundPackage7Days != event.packageName) {
                        launchCount7Days++
                        lastForegroundPackage7Days = event.packageName
                    }
                }
            } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                // Suivre les autres apps pour détecter les changements
                lastForegroundPackage7Days = event.packageName
            }
        }

        // Get stats for last 30 days
        val startTime30Days = endTime - TimeUnit.DAYS.toMillis(30)
        val usageStatsList30Days = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime30Days,
            endTime
        )

        var totalTime30Days = 0L

        // Aggregate stats for the last 30 days
        for (stats in usageStatsList30Days) {
            if (stats.packageName == packageName) {
                totalTime30Days += stats.totalTimeInForeground
            }
        }

        // Count launches for last 30 days
        val events30Days = usageStatsManager.queryEvents(startTime30Days, endTime)
        var launchCount30Days = 0
        var lastForegroundPackage30Days: String? = null
        while (events30Days.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events30Days.getNextEvent(event)

            if (event.packageName == packageName) {
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (lastForegroundPackage30Days != event.packageName) {
                        launchCount30Days++
                        lastForegroundPackage30Days = event.packageName
                    }
                }
            } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                // Suivre les autres apps pour détecter les changements
                lastForegroundPackage30Days = event.packageName
            }
        }

        return AppUsageStats(
            totalTimeToday = totalTimeTodayFromEvents,
            launchCountToday = sessionCount,
            averageSessionTime = averageSessionTime,
            lastUsedTime = appStats.lastTimeUsed,
            totalTimeLast7Days = totalTime7Days,
            launchCountLast7Days = launchCount7Days,
            totalTimeLast30Days = totalTime30Days,
            launchCountLast30Days = launchCount30Days
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

    data class AggregatedStats(
        val totalScreenTimeToday: Long,      // Temps d'écran total aujourd'hui (ms)
        val totalScreenTime7Days: Long,      // Temps d'écran sur 7 derniers jours (ms)
        val totalLaunchCount: Int,           // Nombre total d'ouvertures
        val averageSessionTime: Long,        // Temps moyen par session (ms)
        val timeSavedTotal: Long,            // Temps gagné total = peanuts * averageSessionTime (ms)
        val timeSavedToday: Long,            // Temps gagné aujourd'hui = peanutsToday * averageSessionTime (ms)
        val timeSaved7Days: Long             // Temps gagné sur 7 jours (ms)
    )

    /**
     * Calcule les statistiques agrégées pour toutes les apps configurées
     */
    fun getAggregatedStats(
        context: Context,
        configuredPackages: List<String>,
        peanutCount: Int,
        peanutsToday: Int,
        peanutsLast7Days: Int
    ): AggregatedStats {
        if (!PermissionHelper.hasUsageStatsPermission(context) || configuredPackages.isEmpty()) {
            return AggregatedStats(0, 0, 0, 0, 0, 0, 0)
        }

        var totalTime = 0L
        var totalLaunches = 0
        var totalTime7Days = 0L
        var totalLaunches7Days = 0

        for (packageName in configuredPackages) {
            val stats = getAppUsageStats(context, packageName)
            if (stats != null) {
                totalTime += stats.totalTimeToday
                totalLaunches += stats.launchCountToday
                totalTime7Days += stats.totalTimeLast7Days
                totalLaunches7Days += stats.launchCountLast7Days
            }
        }

        val averageSession = if (totalLaunches > 0) {
            totalTime / totalLaunches
        } else {
            0L
        }

        val averageSession7Days = if (totalLaunches7Days > 0) {
            totalTime7Days / totalLaunches7Days
        } else {
            0L
        }

        val timeSavedTotal = peanutCount * averageSession
        val timeSavedToday = peanutsToday * averageSession
        val timeSaved7Days = peanutsLast7Days * averageSession7Days

        return AggregatedStats(
            totalScreenTimeToday = totalTime,
            totalScreenTime7Days = totalTime7Days,
            totalLaunchCount = totalLaunches,
            averageSessionTime = averageSession,
            timeSavedTotal = timeSavedTotal,
            timeSavedToday = timeSavedToday,
            timeSaved7Days = timeSaved7Days
        )
    }
}
