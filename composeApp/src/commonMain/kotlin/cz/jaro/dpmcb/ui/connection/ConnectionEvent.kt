package cz.jaro.dpmcb.ui.connection

sealed interface ConnectionEvent {
    data class SelectBus(val level: Int) : ConnectionEvent
    data class OnSwipe(val level: Int, val newPage: Int) : ConnectionEvent
}
