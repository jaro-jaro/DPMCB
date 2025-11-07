package cz.jaro.dpmcb.ui.connection

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class ConnectionState(
    val buses: Alternatives,
    val length: Duration,
    val start: LocalDateTime,
    val coordinates: Coordinates,
)

typealias Alternatives = List<ConnectionTree>
fun Alternatives(vararg items: ConnectionTree): Alternatives = items.toList()

operator fun Alternatives.get(coordinates: Coordinates): ConnectionTree {
    require(coordinates.isNotEmpty()) { "At least one coordinate needs to be specified" }
    val first = coordinates.first()
    val rest = coordinates.drop(1)
    val tree = this[first]
    return if (rest.isEmpty()) tree else tree.next[rest]
}

fun Alternatives.getAlternatives(coordinates: Coordinates): Alternatives
    = if (coordinates.isEmpty()) this else get(coordinates).next

data class ConnectionTree(
    val part: ConnectionBus?,
    val next: Alternatives,
    val page: Int,
    val level: Int,
) {
    override fun toString() = "$part ($level->$page) -> $next"
}

data class ConnectionBus(
    val transferTime: Duration?,
    val transferTight: Boolean,
    val bus: BusName,
    val line: ShortLine,
    val isTrolleybus: Boolean,
    val date: LocalDate,
    val startStop: StopName,
    val departure: LocalTime,
    val startStopPlatform: Platform,
    val endStop: StopName,
    val arrival: LocalTime,
    val endStopPlatform: Platform,
    val stopCount: Int,
    val direction: StopName,
    val length: Duration,
) {
    override fun toString() = "$startStop ($departure) -> ($bus) $endStop ($arrival)"
}

typealias Coordinates = List<Int>