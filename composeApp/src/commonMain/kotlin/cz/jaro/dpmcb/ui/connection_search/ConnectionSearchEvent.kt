package cz.jaro.dpmcb.ui.connection_search

import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.ui.chooser.ChooserType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface ConnectionSearchEvent {
    data class ChoseStop(val type: ChooserType) : ConnectionSearchEvent
    data object SwitchStops : ConnectionSearchEvent
    data object Search : ConnectionSearchEvent
    data object ClearAll : ConnectionSearchEvent
    data class WentBack(val type: ChooserType, val stop: StopName) : ConnectionSearchEvent
    data class SearchFavourite(val i: Int) : ConnectionSearchEvent
    data class SearchFromHistory(val i: Int, val includeDatetime: Boolean) : ConnectionSearchEvent
    data class FillFromHistory(val i: Int, val includeDatetime: Boolean = false) : ConnectionSearchEvent
    data class DeleteFromHistory(val i: Int) : ConnectionSearchEvent
    data class SetOnlyDirect(val onlyDirect: Boolean) : ConnectionSearchEvent
    data class SetShowInefficientConnections(val showInefficientConnections: Boolean) : ConnectionSearchEvent
    data class ChangeDate(val date: LocalDate) : ConnectionSearchEvent
    data class ChangeTime(val time: LocalTime) : ConnectionSearchEvent
}
