package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.ChooserResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.JsonPrimitive

sealed interface DeparturesEvent {
    data class GoToBus(val bus: DepartureState) : DeparturesEvent
    data class GoToTimetable(val bus: DepartureState) : DeparturesEvent
    data class ChangeTime(val time: LocalTime?) : DeparturesEvent
    data class Scroll(val i: Int) : DeparturesEvent
    data class WentBack(val result: ChooserResult<JsonPrimitive>) : DeparturesEvent
    data class Canceled(val chooserType: ChooserType) : DeparturesEvent
    data class ChangeDate(val date: LocalDate) : DeparturesEvent
    data object ChangeCompactMode : DeparturesEvent
    data object ChangeJustDepartures : DeparturesEvent
    data object ScrollToHome : DeparturesEvent
    data object NextDay : DeparturesEvent
    data object PreviousDay : DeparturesEvent
    data object ChangeStop : DeparturesEvent
    data object ChangeLine : DeparturesEvent
    data object ChangeVia : DeparturesEvent
    data object ChangePlatform : DeparturesEvent
}
