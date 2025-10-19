package cz.jaro.dpmcb.ui.timetable

import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable
import kotlinx.datetime.LocalDate

sealed interface TimetableState {
    val lineNumber: ShortLine
    val stop: String
    val date: LocalDate
    data class Loading(
        override val date: LocalDate,
        override val lineNumber: ShortLine,
        override val stop: String,
    ) : TimetableState
    data class Success(
        val data: List<BusInTimetable>,
        val endStops: String,
        override val date: LocalDate,
        override val lineNumber: ShortLine,
        override val stop: String,
    ) : TimetableState
}