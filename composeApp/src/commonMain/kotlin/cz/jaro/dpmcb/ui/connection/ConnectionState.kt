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

/**
 * Nodes with a common parent, all on the same level
 */
typealias Alternatives = List<ConnectionTreeNode>

fun Alternatives(vararg items: ConnectionTreeNode): Alternatives = items.toList()

data class ConnectionData(
    val rootAlternatives: Alternatives,
    val currentCoordinates: Coordinates,
)

/**
 * Finds the node specified by the [coordinates] in the tree
 */
operator fun Alternatives.get(coordinates: Coordinates): ConnectionTreeNode {
    require(coordinates.isNotEmpty()) { "At least one coordinate needs to be specified" }
    require(isNotEmpty()) { "Can't get coordinates $coordinates of empty alternatives" }
    val firstCoordinate = coordinates.first()
    val rest = coordinates.drop(1)
    require(firstCoordinate < size) { "Page coordinate $firstCoordinate is out of bounds for $size children. Current level: ${first().level}" }
    val node = this[firstCoordinate]
    return if (rest.isEmpty()) node else node.next[rest]
}

/**
 * @return The children of the node at the [coordinates]. The root alternatives if [coordinates] is empty.
 */
fun Alternatives.getAlternatives(coordinates: Coordinates): Alternatives =
    if (coordinates.isEmpty()) this else get(coordinates).next

/**
 * A node in a tree of buses in a connection
 */
data class ConnectionTreeNode(
    val part: ConnectionBus?,
    /**
     * Children
     */
    val next: Alternatives,
    /**
     * Horizontal coordinate (index in the parent children list)
     */
    val page: Int,
    /**
     * Vertical coordinate
     */
    val level: Int,
) {
    override fun toString() = "$part ($level:$page) -> $next"
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
    val startStopPlatform: Platform?,
    val endStop: StopName,
    val arrival: LocalTime,
    val endStopPlatform: Platform?,
    val stopCount: Int,
    val direction: StopName,
    val length: Duration,
) {
    override fun toString() = "$startStop ($departure) -> ($bus) $endStop ($arrival)"
}

/**
 * A list of the page coordinates in a tree specifying a node
 */
typealias Coordinates = List<Int>