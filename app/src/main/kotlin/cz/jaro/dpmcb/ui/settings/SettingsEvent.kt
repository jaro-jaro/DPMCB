package cz.jaro.dpmcb.ui.settings

import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.helperclasses.MutateLambda

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data object UpdateApp : SettingsEvent
    data object UpdateData : SettingsEvent
    data class EditSettings(val edit: MutateLambda<Settings>) : SettingsEvent
}
