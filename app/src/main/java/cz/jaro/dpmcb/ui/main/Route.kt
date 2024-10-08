package cz.jaro.dpmcb.ui.main

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.hasModifiers
import cz.jaro.dpmcb.data.entities.invalid
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.entities.modifiers
import cz.jaro.dpmcb.data.entities.sequenceNumber
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Route")
sealed interface Route {
    companion object {
        val routes =
            listOf(Bus::class, Card::class, Chooser::class, Departures::class, Favourites::class, Map::class, NowRunning::class, Sequence::class, Timetable::class)
    }

    @Serializable
    @SerialName("bus")
    data class Bus(
        val lineNumber: LongLine,
        val busNumber: BusNumber,
    ) : Route {
        constructor(busName: BusName) : this(busName.line(), busName.bus())
    }

    @Serializable
    @SerialName("card")
    data object Card : Route

    @Serializable
    @SerialName("chooser")
    data class Chooser(
        val type: ChooserType,
        val lineNumber: ShortLine = ShortLine.invalid,
        val stop: String? = null,
    ) : Route

    @Serializable
    @SerialName("departures")
    data class Departures(
        val stop: String,
        val time: SimpleTime = SimpleTime.invalid,
        val line: ShortLine? = null,
        val via: String? = null,
        val onlyDepartures: Boolean? = null,
        val simple: Boolean? = null,
    ) : Route

    @Serializable
    @SerialName("favourites")
    data object Favourites : Route

    @Serializable
    @SerialName("map")
    data object Map : Route

    @Serializable
    @SerialName("now_running")
    data class NowRunning(
        val filters: List<ShortLine> = listOf(),
        val type: NowRunningType = NowRunningType.Line,
    ) : Route

    @Serializable
    @SerialName("sequence")
    data class Sequence(
        val sequenceNumber: String,
        val lineAndModifiers: String,
    ) : Route {
        constructor(sequence: SequenceCode) : this("${sequence.sequenceNumber()}", "${sequence.line()}${if (sequence.hasModifiers()) "-" else ""}${sequence.modifiers()}")
    }

    @Serializable
    @SerialName("timetable")
    data class Timetable(
        val lineNumber: ShortLine,
        val stop: String,
        val nextStop: String,
    ) : Route
}