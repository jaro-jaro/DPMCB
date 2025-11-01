package cz.jaro.dpmcb.ui.connection

import cz.jaro.dpmcb.data.entities.BusName

sealed interface ConnectionEvent {
    data class SelectBus(val bus: BusName) : ConnectionEvent
    data class Swipe(val level: Int, val page: Int) : ConnectionEvent
}
