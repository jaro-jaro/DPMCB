package cz.jaro.dpmcb.ui.favourites

sealed interface FavouritesState {

    data object Loading : FavouritesState

    data class Loaded(
        val recents: List<FavouriteState>?,
        val runsToday: List<FavouriteState>,
        val runsOtherDay: List<FavouriteState>,
    ) : FavouritesState
}
