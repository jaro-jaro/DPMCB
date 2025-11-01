package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.types.VehicleType
import cz.jaro.dpmcb.data.helperclasses.PriorityQueue
import cz.jaro.dpmcb.data.helperclasses.measure
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.work
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.ui.connection_search.SearchSettings
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atDate
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

data class GraphEdge(
//    val from: StopName,
    val departure: LocalTime,
    val arrival: LocalTime,
    val to: StopName,
    val bus: BusName,
    val vehicleType: VehicleType?,
    val departureIndexOnBus: Int,
    val arrivalIndexOnBus: Int,
) {
    override fun toString() = "($departure) -> ($bus) $to ($arrival)"
}

typealias StopGraph = Map<StopName, List<GraphEdge>>
typealias MutableStopGraph = MutableMap<StopName, MutableList<GraphEdge>>
typealias Connection = List<ConnectionPart>

data class ConnectionPart(
    val startStop: StopName,
    val departure: LocalDateTime,
    val departureIndexOnBus: Int,
    val bus: BusName,
    val vehicleType: VehicleType?,
    val arrival: LocalDateTime,
    val arrivalIndexOnBus: Int,
    val endStop: StopName,
) {
    override fun toString() = "$startStop ($departure) -> ($bus) $endStop ($arrival)"
}

private data class SearchTableRow(
    val cameFrom: StopName?,
    val arrival: LocalDateTime,
    val arrivalIndexOnBus: Int,
    val lastDeparture: LocalDateTime?,
    val lastBus: BusName?,
    val vehicleType: VehicleType?,
//    val transfers: Int,
) {
    override fun toString() = "$cameFrom (${lastDeparture?.time}) -> ($lastBus) ${arrival.time}"
}

private typealias SearchTable = Map<StopName, SearchTableRow>
private typealias MutableSearchTable = MutableMap<StopName, SearchTableRow>

class ConnectionSearcher private constructor(
    private val settings: SearchSettings,
    private val strippedDownGraph: StopGraph,
    private val allStops: Set<StopName>,
    private val runsAt: Map<BusName, (LocalDate) -> Boolean>,
    private val stops: Map<BusName, List<StopNameTime>>,
) {
    companion object {
        @Suppress("MayBeConstant", "RedundantSuppression")
        val log = false

        inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.log(vararg msg: R, c: Boolean = true, transform: T.() -> S): T =
            if (log && c) work(*msg, tag = "ConnectionSearcher", transform = transform) else this

        inline fun <reified T : Any?, reified S : Any?> T.log(c: Boolean = true, transform: T.() -> S): T =
            if (log && c) work(tag = "ConnectionSearcher", transform = transform) else this

        inline fun <reified T : Any?, reified R : Any?> T.log(vararg msg: R, c: Boolean = true): T =
            if (log && c) work(*msg, tag = "ConnectionSearcher") else this

        inline fun <T, reified R : Any?> logTime(vararg msg: R, c: Boolean = true, block: () -> T) =
            if (log && c) measure(*msg, tag = "ConnectionSearcher", block = block) else block()

        suspend operator fun invoke(
            settings: SearchSettings,
            repo: SpojeRepository,
        ): ConnectionSearcher {
            settings.log("Vyhledávám")

            val stops: Map<BusName, List<StopNameTime>> = logTime("ČAS: Zastávky nalezeny") {
                repo.stops(settings.datetime.date)
                    .mapKeys { it.key.connName }
            }
            val runsAt: Map<BusName, (LocalDate) -> Boolean> = logTime("ČAS: RunsAt získáno") {
                repo.connsRunAt(settings.datetime.date)
            }
            val stopGraph: StopGraph = logTime("ČAS: Graf získán") {
                repo.stopGraph(settings.datetime.date)
            }

            val pathGraph: Map<StopName, Set<StopName>> = logTime("ČAS: Cesty získány") {
                findPathGraph(
                    start = settings.start,
                    destination = settings.destination,
                    stopGraph = stopGraph.mapValues { (_, edges) -> edges.map { it.to }.toSet() }
                        .log("Graph") { entries.filter { it.value.isNotEmpty() }.joinToString("\n") }
                )
            }.log("PathGraph") { entries.filter { it.value.isNotEmpty() }.joinToString("\n") }

            val strippedDownGraph: StopGraph = logTime("ČAS: Druhý graf získán") {
                stripDownGraph(pathGraph, stopGraph)
                    .log("Graph") { entries.filter { it.value.isNotEmpty() }.joinToString("\n") }
            }
            val allStops: Set<StopName> = strippedDownGraph.keys + settings.destination

            return ConnectionSearcher(settings, strippedDownGraph, allStops, runsAt, stops)
        }

        private fun stripDownGraph(
            pathGraph: Map<StopName, Set<StopName>>,
            stopGraph: StopGraph,
        ): StopGraph {
            val stops = pathGraph.keys

            return stopGraph
                .filter { it.key in stops }
                .mapValues { (v, e) -> e.filter { it.to in pathGraph[v]!! }.sortedBy { it.departure } }
        }

        private fun findPathGraph(
            start: StopName,
            destination: StopName,
            stopGraph: Map<StopName, Set<StopName>>,
        ): Map<StopName, Set<StopName>> {
            deadEnds.clear()
            needConfirmingFrom.clear()
            cache.clear()
            pathGraph.clear()

            doesThisStopLeadToTheDestination(
                thisStop = start,
                destination = destination,
                stopGraph = stopGraph,
            )

            return pathGraph
        }

        private val deadEnds: MutableSet<StopName> = mutableSetOf()
        private val needConfirmingFrom: MutableMap<StopName, Set<StopName>> = mutableMapOf()
        private val cache: MutableMap<Pair<StopName, StopName>, Boolean> = mutableMapOf()
        private val pathGraph: MutableMap<StopName, Set<StopName>> = mutableMapOf()

        @Suppress("MayBeConstant", "RedundantSuppression")
        val logPaths = false

        private fun doesThisStopLeadToTheDestination(
            thisStop: StopName,
            destination: StopName,
            stopGraph: Map<StopName, Set<StopName>>,
            currentPath: List<StopName> = emptyList(),
        ): Boolean {
            val lastStop = currentPath.lastOrNull()

            currentPath.log("Now", thisStop, "Last", lastStop, "So far", c = logPaths)
            needConfirmingFrom.log("Needs", c = logPaths)
            deadEnds.log("Dead ends", c = logPaths)

            if (thisStop in deadEnds) {
                "is a dead end, skipping".log(thisStop, c = logPaths)
                return false
            }

            if ((lastStop to thisStop) in cache.keys) {
                return cache[lastStop to thisStop]!!.log(thisStop, "is cached, returning", c = logPaths)
            }

            if (thisStop == destination) {
                "is the destination, ending".log(thisStop, c = logPaths)
                return true
            }

            // Don't go back
            val allDirections = stopGraph[thisStop]!!.filter { lastStop != it }.log("All directions", c = logPaths)

            // Don't loop
            val directions = allDirections.filterNot { it in currentPath || it in deadEnds }
                .log("Directions to search", c = logPaths)

            val nextOptions = directions.filter { direction ->
                thisStop.log("Searching", direction, "From", c = logPaths)
                doesThisStopLeadToTheDestination(
                    thisStop = direction,
                    destination = destination,
                    stopGraph = stopGraph,
                    currentPath = currentPath + listOf(thisStop)
                )
            }.log(thisStop, "got further options", c = logPaths)

            if (nextOptions.isEmpty() && allDirections.all { it in deadEnds }) {
                allDirections.log(thisStop, "is a complete dead-end; directions", c = logPaths)
                needConfirmingFrom.remove(thisStop)
                deadEnds += thisStop
            } else if (lastStop in (needConfirmingFrom[thisStop]
                    ?: emptySet()) && nextOptions.isEmpty() && allDirections.all { it in deadEnds || it in currentPath }
            ) {
                allDirections.log(thisStop, "Confirmed from $lastStop; directions", c = logPaths)
                needConfirmingFrom[thisStop] = needConfirmingFrom[thisStop]!! - lastStop!!
                if (needConfirmingFrom[thisStop]!!.isEmpty()) {
                    allDirections.log(thisStop, "Now confirmed from all directions, marking as a dead end; directions", c = logPaths)
                    needConfirmingFrom.remove(thisStop)
                    deadEnds += thisStop
                }
            } else if (nextOptions.isEmpty() && allDirections.all { it in deadEnds || it in currentPath || it in needConfirmingFrom.keys }) {
                val troublesomeDirections = allDirections
                    .filter { it in currentPath || it in needConfirmingFrom.keys }
                    .filter { thisStop in stopGraph.getValue(it) }
                allDirections.log(thisStop, "Needs confirming from", troublesomeDirections, "all directions", c = logPaths)
                if (troublesomeDirections.isNotEmpty())
                    needConfirmingFrom[thisStop] = (needConfirmingFrom[thisStop] ?: emptySet()) + troublesomeDirections
            }
            needConfirmingFrom.log("Needs", c = logPaths)
            deadEnds.log("Dead ends", c = logPaths)

            pathGraph[thisStop] = (pathGraph[thisStop] ?: emptySet()) + nextOptions

            val result = nextOptions.isNotEmpty()

            val canBeCached = allDirections.none { it in currentPath } && lastStop != null
            if (canBeCached) {
                cache[lastStop to thisStop] = result.log(thisStop, "Can be cached; result", c = logPaths)
            }
            return result
        }
    }

    fun search(
        firstOffset: Int,
        count: Int,
        start: StopName? = null,
        datetime: LocalDateTime? = null,
    ) = search(
        firstOffset = firstOffset, count = count,
        searchToPast = false, start = start, datetime = datetime,
    )

    fun searchBack(
        firstOffset: Int,
        count: Int,
    ) = search(
        firstOffset = firstOffset, count = count,
        searchToPast = true, start = null, datetime = null,
    )

    private fun search(
        firstOffset: Int,
        count: Int,
        searchToPast: Boolean,
        start: StopName?,
        datetime: LocalDateTime?,
    ) = flow {
        logTime("ČAS: Nalezeno všech $count spojení") {
            repeat(count) { i ->
                val skip = i + firstOffset
                logTime("ČAS: Spojení č. $skip nalezeno") {
                    val connection = find(
                        offset = skip,
                        searchToPast = searchToPast,
                        settings = settings.copy(
                            start = start ?: settings.start,
                            datetime = datetime ?: settings.datetime,
                        ),
                        strippedDownGraph = strippedDownGraph,
                        allStops = allStops,
                        runsAt = runsAt,
                        stops = stops
                    )
                    emit(connection)
                }
            }
        }
    }

    private fun find(
        offset: Int,
        searchToPast: Boolean,
        settings: SearchSettings,
        strippedDownGraph: StopGraph,
        allStops: Set<StopName>,
        runsAt: Map<BusName, (LocalDate) -> Boolean>,
        stops: Map<BusName, List<StopNameTime>>,
    ): Connection? {
        val searchTable = searchTroughGraph(
            settings = settings,
            offset = offset,
            searchToPast = searchToPast,
            graph = strippedDownGraph,
            allStops = allStops,
            runsAt = runsAt,
        )

        if (searchTable[settings.destination]!!.cameFrom == null) {
            return null
        }

        val connection = searchTable.flattenTable(
            destination = settings.destination,
            startStop = settings.start
        )

        val joint = connection.joinSameBus()
            .log("CONNECTION before optimization") { joinToString() }

        val extended = joint.extendBusesFromEnd(
            stops = stops,
        ).log("CONNECTION") { joinToString() }

        return extended
    }

    private fun Connection.joinSameBus(): Connection = this
        .groupBy { it.bus }
        .map { (bus, parts) ->
            val first = parts.first()
            val last = parts.last()
            ConnectionPart(
                startStop = first.startStop,
                departure = first.departure,
                departureIndexOnBus = first.departureIndexOnBus,
                bus = bus,
                vehicleType = first.vehicleType,
                arrival = last.arrival,
                arrivalIndexOnBus = last.arrivalIndexOnBus,
                endStop = last.endStop,
            )
        }

    private fun searchTroughGraph(
        settings: SearchSettings,
        offset: Int,
        searchToPast: Boolean,
        graph: StopGraph,
        allStops: Set<StopName>,
        runsAt: Map<BusName, (date: LocalDate) -> Boolean>,
    ): SearchTable {
        val searchTable: MutableSearchTable = (mapOf(
            settings.start to SearchTableRow(
                cameFrom = null,
                arrival = settings.datetime,
                arrivalIndexOnBus = -1,
                lastDeparture = null,
                lastBus = null,
                vehicleType = null,
//            transfers = 0,
            ),
        ) + (allStops - settings.start).associateWith {
            SearchTableRow(
                cameFrom = null,
                arrival = settings.datetime + 100.days,
                arrivalIndexOnBus = -1,
                lastDeparture = null,
                lastBus = null,
                vehicleType = null,
//            transfers = Int.MAX_VALUE,
            )
        }).toMutableMap()

        val queue = PriorityQueue<Pair<StopName, LocalDateTime>>(compareBy { it.second })
        queue.offer(settings.start to settings.datetime)

        while (!queue.isEmpty()) {
            val (thisStop) = queue.poll()!!
            if (thisStop == settings.destination) continue

            val row = searchTable[thisStop]!!
            val currentResult = searchTable[settings.destination]!!.arrival

            val allOptions = graph[thisStop.log("Current")]!!

            val willSearchToPast = thisStop == settings.start && searchToPast

            val yesterday by lazy { allOptions.map { it to row.arrival.date - 1.days } }
            val today by lazy { allOptions.map { it to row.arrival.date } }
            val tomorrow by lazy { allOptions.map { it to row.arrival.date + 1.days } }
            val datedOptions = if (willSearchToPast) yesterday + today else today + tomorrow

            val runningOptions = datedOptions
                .filter { (it, date) ->
                    val departure = it.departure.atDate(date)
                    val runs = it.to in allStops && runsAt[it.bus]!!(date)
                    if (willSearchToPast) departure < row.arrival && runs
                    else row.arrival <= departure && runs
                }

            val noTransfer = if (row.lastBus != null) runningOptions.find { (edge) ->
                edge.bus == row.lastBus && edge.departureIndexOnBus == row.arrivalIndexOnBus
            } else null

            val offsetOptions =
                (if (willSearchToPast) runningOptions.dropLast(offset)
                else runningOptions.drop(if (thisStop == settings.start) offset else 0))
                    .log("Current", thisStop, "Next options") { map { it.first } }

            offsetOptions
                .groupBy { it.first.to }
                .forEach { (to, options) ->
                    val next = noTransfer?.takeIf { it.first.to == to }
                        ?: if (willSearchToPast) options.lastOrNull() else options.firstOrNull()

                    next
                        ?.takeIf { (edge, date) ->
                            val current = searchTable[to]!!.arrival
                            val new = edge.arrival.atDate(date).log(
                                "Current", row.arrival.time, thisStop, edge.departure, "Next", to,
                                "Using", edge.bus, "No transfer", noTransfer?.first?.bus,
                                "Best time", current.time, "New time"
                            ) { time }
                            new < current && new < currentResult
                        }
                        ?.let { (edge, date) ->
                            searchTable[to] = SearchTableRow(
                                cameFrom = thisStop,
                                arrival = edge.arrival.atDate(date),
                                arrivalIndexOnBus = edge.arrivalIndexOnBus,
                                lastBus = edge.bus,
                                vehicleType = edge.vehicleType,
                                lastDeparture = edge.departure.atDate(date),
//                            transfers = if (noTransfer == null) row.transfers + 1 else row.transfers
                            )
                            queue.offer(to to edge.arrival.atDate(date))
                        }
                }
        }

        return searchTable.log { entries.filter { it.value.lastBus != null }.joinToString("\n") }
    }

    @OptIn(ExperimentalTime::class)
    private fun Connection.extendBusesFromEnd(
        stops: Map<BusName, List<StopNameTime>>,
    ): Connection {
        val newParts = toMutableList()
        var currentIndex = newParts.size

        while (--currentIndex > 0) {
            val thisPart = newParts[currentIndex]
            val previousPart = newParts[currentIndex - 1]

            val busStops = stops.getValue(thisPart.bus)
            val stopsBeforeThisPart = busStops.slice(0..<thisPart.departureIndexOnBus)

            val lastPartDepartureOnThisBus = stopsBeforeThisPart.withIndex()
                .findLast { it.value.name == previousPart.startStop && it.value.time >= previousPart.departure.time }

            if (lastPartDepartureOnThisBus == null)
                continue

            newParts[currentIndex] = thisPart.copy(
                startStop = lastPartDepartureOnThisBus.value.name,
                departure = lastPartDepartureOnThisBus.value.time.atDate(thisPart.departure.date),
                departureIndexOnBus = lastPartDepartureOnThisBus.index
            )
            newParts.removeAt(currentIndex - 1)
        }

        return newParts
    }

    private fun Map<StopName, SearchTableRow>.flattenTable(
        destination: StopName,
        startStop: StopName,
    ): Connection {
        val reversedResult = mutableListOf<ConnectionPart>()
        var currentStop = destination

        do {
            val row = getValue(currentStop)

            reversedResult += ConnectionPart(
                startStop = row.cameFrom!!,
                departure = row.lastDeparture!!,
                departureIndexOnBus = row.arrivalIndexOnBus - 1,
                bus = row.lastBus!!,
                vehicleType = row.vehicleType,
                arrival = row.arrival,
                arrivalIndexOnBus = row.arrivalIndexOnBus,
                endStop = currentStop,
            )

            currentStop = row.cameFrom
        } while (currentStop != startStop)

        return reversedResult.reversed()
    }
}
