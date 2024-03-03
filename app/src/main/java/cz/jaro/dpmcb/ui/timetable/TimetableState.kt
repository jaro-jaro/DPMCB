package cz.jaro.dpmcb.ui.timetable

import cz.jaro.dpmcb.data.realtions.TimeLowFloorConnIdDestinationFixedCodesDelay

sealed interface TimetableState {
    data object Loading : TimetableState
    data class Success(val data: List<TimeLowFloorConnIdDestinationFixedCodesDelay>) : TimetableState
}