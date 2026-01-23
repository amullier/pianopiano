package fr.antmu.pianopiano.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import fr.antmu.pianopiano.data.model.AppUsageStats
import java.util.Calendar

class UsageStatsRepository(context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    fun getUsageStats(daysBack: Int = 7): List<AppUsageStats> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .map { (packageName, statsList) ->
                val totalTime = statsList.sumOf { it.totalTimeInForeground }
                val lastUsed = statsList.maxOf { it.lastTimeUsed }
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }

                AppUsageStats(
                    packageName = packageName,
                    appName = appName,
                    totalTimeInForeground = totalTime,
                    lastTimeUsed = lastUsed
                )
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    fun hasUsageStatsPermission(): Boolean {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        return stats.isNotEmpty()
    }
}
