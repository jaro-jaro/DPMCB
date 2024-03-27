package cz.jaro.dpmcb.ui.settings

import cz.jaro.dpmcb.data.Settings

data class SettingsState(
    val settings: Settings,
    val version: String,
    val dataVersion: Int,
    val dataMetaVersion: Int,
)
