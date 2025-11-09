package cz.jaro.dpmcb.ui.main

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.BusNumber
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.div
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
import cz.jaro.dpmcb.ui.common.toSimpleTime
import cz.jaro.dpmcb.ui.connection.ConnectionDefinition
import cz.jaro.dpmcb.ui.connection_search.Relations
import cz.jaro.dpmcb.ui.connection_search.SearchSettings
import cz.jaro.dpmcb.ui.connection_search.to
import cz.jaro.dpmcb.ui.now_running.NowRunningType
import io.github.z4kn4fein.semver.Version
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
            Bus::class, Card::class, Chooser::class, Departures::class, ConnectionSearch::class, Connection::class,
            ConnectionResults::class, Map::class, NowRunning::class, Sequence::class, Timetable::class, FindBus::class, Settings::class
        )
    }

    @Serializable
    @SerialName("bus")
    data class Bus(
        override val date: LocalDate,
        val lineNumber: LongLine,
        val busNumber: BusNumber,
        val from: Int? = null,
        val to: Int? = null,
    ) : Route {
        constructor(date: LocalDate, busName: BusName, from: Int? = null, to: Int? = null)
                : this(date, busName.line(), busName.bus(), from, to)

        val busName get() = lineNumber / busNumber
        val part get() = if (from != null && to != null) from..to else null
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
    @SerialName("map")
    data class Map(
        override val date: LocalDate,
    ) : Route

    @Serializable
    @SerialName("connection_search")
    data class ConnectionSearch(
        override val date: LocalDate,
        val time: SimpleTime? = null,
        val start: StopName? = null,
        val destination: StopName? = null,
        val directOnly: Boolean? = null,
        val showInefficientConnections: Boolean? = null,
    ) : Route

    @Serializable
    @SerialName("connection_results")
    data class ConnectionResults(
        override val date: LocalDate,
        val time: SimpleTime,
        val relations: Relations,
        val directOnly: Boolean = false,
        val showInefficientConnections: Boolean = false,
    ) : Route {
        constructor(settings: SearchSettings) : this(
            date = settings.datetime.date,
            time = settings.datetime.time.toSimpleTime(),
            relations = Relations(listOf(settings.start to settings.destination)),
            directOnly = settings.directOnly,
            showInefficientConnections = settings.showInefficientConnections,
        )
    }

    @Serializable
    @SerialName("connection")
    data class Connection(
        val def: ConnectionDefinition,
    ) : Route {
        override val date: LocalDate get() = def.first().date
    }

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
        val appVersionToUpdate: Version? = null,
    ) : SuperRoute
}