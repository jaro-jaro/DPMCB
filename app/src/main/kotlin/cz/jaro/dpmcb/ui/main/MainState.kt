package cz.jaro.dpmcb.ui.main

data class MainState(
    val onlineStatus: OnlineStatus,
    val hasCard: Boolean = false,
) {
    sealed interface OnlineStatus {
        data object Offline : OnlineStatus
        data class Online(val onlineMode: Boolean) : OnlineStatus
    }
}
