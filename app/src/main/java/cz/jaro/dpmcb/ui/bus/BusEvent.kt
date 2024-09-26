package cz.jaro.dpmcb.ui.bus

import cz.jaro.dpmcb.data.realtions.favourites.PartOfConn
import cz.jaro.dpmcb.ui.common.TimetableEvent
import kotlinx.datetime.LocalDate

sealed interface BusEvent {
    data class ChangeFavourite(val newFavourite: PartOfConn) : BusEvent
    data object RemoveFavourite : BusEvent
    data class ChangeDate(val date: LocalDate) : BusEvent
    data object PreviousBus : BusEvent
    data object NextBus : BusEvent
    data object ShowSequence : BusEvent
    data class TimetableClick(val e: TimetableEvent) : BusEvent
}

val ((event: BusEvent) -> Unit).fromTimetable get() = { event: TimetableEvent -> this(BusEvent.TimetableClick(event)) }