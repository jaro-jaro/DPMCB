package cz.jaro.dpmcb.ui.favourites

sealed interface FavouritesState {

    data object Loading : FavouritesState

    data object NoFavourites : FavouritesState

    sealed interface SomethingRunsSometime : FavouritesState

    sealed interface SomethingRunsToday : SomethingRunsSometime {
        val runsToday: List<FavouriteState>
    }

    sealed interface SomethingRunsOtherDay : SomethingRunsSometime {
        val runsOtherDay: List<FavouriteState.Offline>
    }

    data class RunsJustToday(
        override val runsToday: List<FavouriteState>,
    ) : SomethingRunsToday

    data class NothingRunsToday(
        override val runsOtherDay: List<FavouriteState.Offline>,
    ) : SomethingRunsOtherDay

    data class RunsAnytime(
        override val runsToday: List<FavouriteState>,
        override val runsOtherDay: List<FavouriteState.Offline>,
    ) : SomethingRunsToday, SomethingRunsOtherDay
}
