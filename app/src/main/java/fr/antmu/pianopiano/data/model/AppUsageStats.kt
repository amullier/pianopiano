package fr.antmu.pianopiano.data.model

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long
)
