package cz.jaro.dpmcb.ui.settings

import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.helperclasses.MutateLambda

sealed interface SettingsEvent {
    data class UpdateApp(val loadingDialog: (String?) -> Unit) : SettingsEvent
    data object UpdateData : SettingsEvent
    data class EditSettings(val edit: MutateLambda<Settings>) : SettingsEvent
}
