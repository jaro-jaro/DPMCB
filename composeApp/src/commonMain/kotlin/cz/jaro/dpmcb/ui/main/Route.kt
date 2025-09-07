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
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.chooser.ChooserType
import cz.jaro.dpmcb.ui.common.SimpleTime
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
@SerialName("Route")
@OptIn(ExperimentalTime::class)
sealed interface Route {
    val date: LocalDate

    companion object {
        val routes = listOf(
            Bus::class, Card::class, Chooser::class, Departures::class, Favourites::class, Map::class,
            NowRunning::class, Sequence::class, Timetable::class, FindBus::class, Settings::class
        )
    }

    @Serializable
    @SerialName("bus")
    data class Bus(
        override val date: LocalDate,
        val lineNumber: LongLine,
        val busNumber: BusNumber,
    ) : Route {
        constructor(date: LocalDate, busName: BusName) : this(date, busName.line(), busName.bus())
    }

    @Serializable
    @SerialName("card")
    data object Card : Route {
        override val date: LocalDate get() = SystemClock.todayHere()
    }

    @Serializable
    @SerialName("chooser")
    data class Chooser(
        override val date: LocalDate,
        val type: ChooserType,
        val lineNumber: ShortLine = ShortLine.invalid,
        val stop: String? = null,
    ) : Route

    @Serializable
    @SerialName("departures")
    data class Departures(
        override val date: LocalDate,
        val stop: String,
        val time: SimpleTime = SimpleTime.invalid,
        val line: ShortLine? = null,
        val via: String? = null,
        val onlyDepartures: Boolean? = null,
        val simple: Boolean? = null,
    ) : Route

    @Serializable
    @SerialName("favourites")
    data object Favourites : Route {
        override val date: LocalDate get() = SystemClock.todayHere()
    }

    @Serializable
    @SerialName("map")
    data class Map(
        override val date: LocalDate,
    ) : Route

    @Serializable
    @SerialName("now_running")
    data class NowRunning(
        val filters: List<ShortLine> = listOf(),
        val type: NowRunningType = NowRunningType.Line,
    ) : Route {
        override val date: LocalDate get() = SystemClock.todayHere()
    }

    @Serializable
    @SerialName("sequence")
    data class Sequence(
        override val date: LocalDate,
        val sequenceNumber: String,
        val lineAndModifiers: String,
    ) : Route {
        constructor(date: LocalDate, sequence: SequenceCode) : this(
            date,
            "${sequence.sequenceNumber()}",
            "${sequence.line()}${if (sequence.hasModifiers()) "-" else ""}${sequence.modifiers()}",
        )
    }

    @Serializable
    @SerialName("timetable")
    data class Timetable(
        override val date: LocalDate,
        val lineNumber: ShortLine,
        val stop: String,
        val direction: Direction,
    ) : Route

    @Serializable
    @SerialName("search")
    data class FindBus(
        override val date: LocalDate,
    ) : Route

    @Serializable
    @SerialName("settings")
    data object Settings : Route {
        override val date: LocalDate get() = SystemClock.todayHere()
    }
}

@Serializable
@SerialName("SuperRoute")
sealed interface SuperRoute {

    @Serializable
    @SerialName("loading")
    data class Loading(
        val link: String?,
        val update: Boolean? = null,
    ) : SuperRoute

    @Serializable
    @SerialName("main")
    data class Main(
        val link: String?,
        val isDataUpdateNeeded: Boolean = false,
        val isAppDataUpdateNeeded: Boolean = false,
    ) : SuperRoute
}