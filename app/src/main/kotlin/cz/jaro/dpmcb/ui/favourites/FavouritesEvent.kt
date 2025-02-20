package cz.jaro.dpmcb.ui.favourites

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.datetime.LocalDate

sealed interface FavouritesEvent {
    data class NavToBus(val name: BusName, val nextWillRun: LocalDate?) : FavouritesEvent
}