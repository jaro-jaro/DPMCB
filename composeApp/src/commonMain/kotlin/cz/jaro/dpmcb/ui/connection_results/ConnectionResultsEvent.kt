package cz.jaro.dpmcb.ui.connection_results

import cz.jaro.dpmcb.ui.connection.ConnectionDefinition
import kotlinx.datetime.LocalDate

sealed interface ConnectionResultsEvent {
    data class SelectConnection(val def: ConnectionDefinition, val startDate: LocalDate) : ConnectionResultsEvent
    data object LoadPast : ConnectionResultsEvent
    data object LoadMore : ConnectionResultsEvent
}
