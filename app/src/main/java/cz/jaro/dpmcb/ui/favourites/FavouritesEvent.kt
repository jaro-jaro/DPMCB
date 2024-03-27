package cz.jaro.dpmcb.ui.favourites

import java.time.LocalDate

sealed interface FavouritesEvent {
    data class NavToBusToday(val id: String) : FavouritesEvent
    data class NavToBusOtherDay(val id: String, val nextWillRun: LocalDate?) : FavouritesEvent
}