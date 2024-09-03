package cz.jaro.dpmcb.ui.sequence

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.ui.common.TimetableEvent

sealed interface SequenceEvent {
    data class BusClick(val busName: BusName) : SequenceEvent
    data class SequenceClick(val sequence: SequenceCode) : SequenceEvent
    data class TimetableClick(val e: TimetableEvent) : SequenceEvent
}

val ((event: SequenceEvent) -> Unit).fromTimetable get() = { event: TimetableEvent -> this(SequenceEvent.TimetableClick(event)) }