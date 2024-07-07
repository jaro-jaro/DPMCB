package cz.jaro.dpmcb.ui.favourites

import java.time.LocalDate

sealed interface FavouritesEvent {
    data class NavToBusToday(val name: String) : FavouritesEvent
    data class NavToBusOtherDay(val name: String, val nextWillRun: LocalDate?) : FavouritesEvent
}