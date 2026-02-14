package cz.jaro.dpmcb.ui.timetable

import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable
import kotlinx.datetime.LocalDate

sealed interface TimetableState {
    val lineNumber: LongLine
    val stop: StopName
    val platform: Platform
    val date: LocalDate
    data class Loading(
        override val date: LocalDate,
        override val lineNumber: LongLine,
        override val stop: StopName,
        override val platform: Platform,
    ) : TimetableState
    data class Success(
        val data: List<BusInTimetable>,
        val endStops: String,
        override val date: LocalDate,
        override val lineNumber: LongLine,
        override val stop: StopName,
        override val platform: Platform,
    ) : TimetableState
}