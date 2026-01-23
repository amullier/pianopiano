package fr.antmu.pianopiano.data.model

data class ConfiguredApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val periodicTimerSeconds: Int = 0  // 0=désactivé, 30, 300, 900, 1800, 3600
)
