package cz.jaro.dpmcb.ui.favourites

import java.time.LocalDate

sealed interface FavouritesState {

    data object Loading : FavouritesState

    data object NoFavourites : FavouritesState

    sealed interface SomethingRunsSometime : FavouritesState {
        val today: LocalDate
    }

    sealed interface SomethingRunsToday : SomethingRunsSometime {
        val runsToday: List<FavouriteState>
    }

    sealed interface SomethingRunsOtherDay : SomethingRunsSometime {
        val runsOtherDay: List<FavouriteState.Offline>
    }

    data class RunsJustToday(
        override val runsToday: List<FavouriteState>,
        override val today: LocalDate,
    ) : SomethingRunsToday

    data class NothingRunsToday(
        override val runsOtherDay: List<FavouriteState.Offline>,
        override val today: LocalDate,
    ) : SomethingRunsOtherDay

    data class RunsAnytime(
        override val runsToday: List<FavouriteState>,
        override val runsOtherDay: List<FavouriteState.Offline>,
        override val today: LocalDate,
    ) : SomethingRunsToday, SomethingRunsOtherDay
}
