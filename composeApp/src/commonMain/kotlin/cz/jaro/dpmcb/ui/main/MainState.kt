package cz.jaro.dpmcb.ui.main

import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.todayHere
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class MainState(
    val onlineStatus: OnlineStatus,
    val date: LocalDate = SystemClock.todayHere(),
    val hasCard: Boolean = false,
    val canGoBack: Boolean = false,
    val isDebug: Boolean,
) {
    sealed interface OnlineStatus {
        data object Offline : OnlineStatus
        data class Online(val onlineMode: Boolean) : OnlineStatus
    }
}
