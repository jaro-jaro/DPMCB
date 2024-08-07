package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.ui.common.Result
import cz.jaro.dpmcb.ui.chooser.ChooserType
import java.time.LocalTime

sealed interface DeparturesEvent {
    data class GoToBus(val bus: DepartureState) : DeparturesEvent
    data class GoToTimetable(val bus: DepartureState) : DeparturesEvent
    data class ChangeTime(val time: LocalTime) : DeparturesEvent
    data class Scroll(val i: Int) : DeparturesEvent
    data class WentBack(val result: Result) : DeparturesEvent
    data class Canceled(val chooserType: ChooserType) : DeparturesEvent
    data object ChangeCompactMode : DeparturesEvent
    data object ChangeJustDepartures : DeparturesEvent
    data object ScrollToHome : DeparturesEvent
    data object NextDay : DeparturesEvent
    data object PreviousDay : DeparturesEvent
}
