package cz.jaro.dpmcb.ui.main

sealed interface MainEvent {
    data object ToggleDrawer : MainEvent
    data object NavigateBack : MainEvent
    data class DrawerItemClicked(val action: DrawerAction) : MainEvent
    data object ToggleOnlineMode : MainEvent
    data object RemoveCard : MainEvent
    data object UpdateData : MainEvent
    data class UpdateApp(val loadingDialog: (String?) -> Unit) : MainEvent
}