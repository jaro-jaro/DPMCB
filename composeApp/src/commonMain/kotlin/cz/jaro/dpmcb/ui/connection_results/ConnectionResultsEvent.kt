package cz.jaro.dpmcb.ui.connection_results

import cz.jaro.dpmcb.ui.connection.ConnectionDefinition

sealed interface ConnectionResultsEvent {
    data class SelectConnection(val def: ConnectionDefinition) : ConnectionResultsEvent
    data object AddToFavourites : ConnectionResultsEvent
    data object RemoveFromFavourites : ConnectionResultsEvent
    data class AddToOtherFavourite(val i: Int) : ConnectionResultsEvent
    data class RemoveFromOtherFavourite(val i: Int) : ConnectionResultsEvent
    data object LoadPast : ConnectionResultsEvent
    data object LoadMore : ConnectionResultsEvent
}
