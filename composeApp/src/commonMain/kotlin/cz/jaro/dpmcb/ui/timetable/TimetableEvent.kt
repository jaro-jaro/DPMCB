package cz.jaro.dpmcb.ui.timetable

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.datetime.LocalDate

sealed interface TimetableEvent {
    data class GoToBus(val bus: BusName) : TimetableEvent
    data class ChangeDate(val date: LocalDate) : TimetableEvent
}