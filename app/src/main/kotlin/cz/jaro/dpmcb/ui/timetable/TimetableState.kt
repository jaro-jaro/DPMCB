package cz.jaro.dpmcb.ui.timetable

import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable
import kotlinx.datetime.LocalDate

sealed interface TimetableState {
    val lineNumber: ShortLine
    val stop: String
    val nextStop: String
    val showLowFloorFromLastTime: Boolean
    val date: LocalDate
    data class Loading(
        override val date: LocalDate,
        override val lineNumber: ShortLine,
        override val stop: String,
        override val nextStop: String,
        override val showLowFloorFromLastTime: Boolean,
    ) : TimetableState
    data class Success(
        val data: List<BusInTimetable>,
        override val date: LocalDate,
        override val lineNumber: ShortLine,
        override val stop: String,
        override val nextStop: String,
        override val showLowFloorFromLastTime: Boolean,
    ) : TimetableState
}