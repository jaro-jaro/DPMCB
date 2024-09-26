package cz.jaro.dpmcb.ui.favourites

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.datetime.LocalDate

sealed interface FavouritesEvent {
    data class NavToBusToday(val name: BusName) : FavouritesEvent
    data class NavToBusOtherDay(val name: BusName, val nextWillRun: LocalDate?) : FavouritesEvent
}