package fr.antmu.pianopiano.data.model

data class ConfiguredApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true
)
