package cz.jaro.dpmcb.ui.timetable

import cz.jaro.dpmcb.data.realtions.timetable.BusInTimetable

sealed interface TimetableState {
    data object Loading : TimetableState
    data class Success(val data: List<BusInTimetable>) : TimetableState
}