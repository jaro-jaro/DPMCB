package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.types.VehicleType
import cz.jaro.dpmcb.data.helperclasses.PriorityQueue
import cz.jaro.dpmcb.data.helperclasses.minus
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.data.realtions.canGetOff
import cz.jaro.dpmcb.data.realtions.canGetOn
import cz.jaro.dpmcb.data.realtions.connection.GraphBus
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.data.realtions.invoke
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atDate
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Suppress("MayBeConstant", "RedundantSuppression")
val log = false

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T, reified R, reified S> T.log(vararg msg: R, c: Boolean = true, transform: T.() -> S): T =
    if (log && c) work(*msg, tag = "ConnectionSearcher", transform = transform) else this@log

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T, reified S> T.log(c: Boolean = true, transform: T.() -> S): T =
    if (log && c) work(tag = "ConnectionSearcher", transform = transform) else this@log

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T, reified R> T.log(vararg msg: R, c: Boolean = true): T =
    if (log && c) work(*msg, tag = "ConnectionSearcher") else this@log

@IgnorableReturnValue
context(logger: Logger)
inline fun <reified T, reified R> logTime(vararg msg: R, c: Boolean = true, block: () -> T) =
    if (log && c) measure(*msg, tag = "ConnectionSearcher", block = block) else block()

data class GraphEdge(
//    val from: StopName,
    val departure: LocalTime,
    val arrival: LocalTime,
    val to: StopName,
    val bus: BusName,
    val vehicleType: VehicleType?,
    val departureIndexOnBus: Int,
    val arrivalIndexOnBus: Int,
    val departurePlatform: Platform?,
    val arrivalPlatform: Platform?,
) {
    override fun toString() = "($departure) -> ($bus) $to ($arrival)"
}

typealias StopGraph = Map<StopName, List<GraphEdge>>
typealias MutableStopGraph = MutableMap<StopName, MutableList<GraphEdge>>
typealias Connection = List<ConnectionPart>

data class ConnectionPart(
    val startStop: StopName,
    val departure: LocalTime,
    val departureIndexOnBus: Int,
    val departurePlatform: Platform?,
    val bus: BusName,
    val vehicleType: VehicleType?,
    val date: LocalDate,
    val arrival: LocalTime,
    val arrivalIndexOnBus: Int,
    val arrivalPlatform: Platform?,
    val endStop: StopName,
) {
    override fun toString() = "$startStop ($departure) -> ($bus) $endStop ($arrival)"
}

private data class SearchTableRow(
    val cameFrom: StopName?,
    val arrival: LocalTime,
    val arrivalIndexOnBus: Int,
    val arrivalPlatform: Platform?,
    val lastDeparture: LocalTime?,
    val lastDeparturePlatform: Platform?,
    val lastBus: BusName?,
    val vehicleType: VehicleType?,
    val date: LocalDate,
//    val transfers: Int,
) {
    override fun toString() = "$cameFrom ($lastDeparture) -> ($lastBus) $arrival"
}

private typealias SearchTable = Map<StopName, SearchTableRow>
private typealias MutableSearchTable = MutableMap<StopName, SearchTableRow>

interface ConnectionSearcher {
    interface Companion {
        context(logger: Logger)
        suspend operator fun invoke(
            start: StopName,
            destination: StopName,
            datetime: LocalDateTime,
            repo: SpojeRepository,
        ): ConnectionSearcher
    }

    fun search(
        firstOffset: Int,
        count: Int,
    ): Flow<Connection?>

    fun searchBack(
        firstOffset: Int,
        count: Int,
    ): Flow<Connection?>
}

class CommonConnectionSearcher private constructor(
    private val start: StopName,
    private val destination: StopName,
    private val datetime: LocalDateTime,
    private val strippedDownGraph: StopGraph,
    private val allStops: Set<StopName>,
    private val runsAt: Map<BusName, (LocalDate) -> Boolean>,
    private val stops: Map<BusName, List<StopNameTime>>,
    logger: Logger,
) : ConnectionSearcher, Logger by logger {
    companion object : ConnectionSearcher.Companion {
        context(logger: Logger)
        override suspend operator fun invoke(
            start: StopName,
            destination: StopName,
            datetime: LocalDateTime,
            repo: SpojeRepository,
        ): CommonConnectionSearcher {
            datetime.log("Vyhledávám", start, destination)

            val stops: Map<BusName, List<StopNameTime>> = logTime("ČAS: Zastávky nalezeny") {
                repo.stops(datetime.date).await()
                    .mapKeys { it.key.connName }
            }
            val runsAt: Map<BusName, (LocalDate) -> Boolean> = logTime("ČAS: RunsAt získáno") {
                repo.connsRunAt(datetime.date).await()
            }
            val stopGraph: StopGraph = logTime("ČAS: Graf získán") {
                repo.stopGraph(datetime.date).await()
            }

            val pathGraph: Map<StopName, Set<StopName>> = logTime("ČAS: Cesty získány") {
                findPathGraph(
                    start = start,
                    destination = destination,
                    stopGraph = stopGraph.mapValues { (_, edges) -> edges.map { it.to }.toSet() }
                        .log("Graph") { entries.filter { it.value.isNotEmpty() }.joinToString("\n") }
                )
            }.log("PathGraph") { entries.filter { it.value.isNotEmpty() }.joinToString("\n") }

            val strippedDownGraph: StopGraph = logTime("ČAS: Druhý graf získán") {
                stripDownGraph(pathGraph, stopGraph)
                    .log("Graph") { entries.filter { it.value.isNotEmpty() }.joinToString("\n") }
            }
            val allStops: Set<StopName> = strippedDownGraph.keys + destination

            return CommonConnectionSearcher(start, destination, datetime, strippedDownGraph, allStops, runsAt, stops, logger)
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

        context(logger: Logger)
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

        context(logger: Logger)
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

    override fun search(
        firstOffset: Int,
        count: Int,
    ) = search(
        firstOffset = firstOffset, count = count,
        searchToPast = false, startOverride = null, datetimeOverride = null,
    )

    override fun searchBack(
        firstOffset: Int,
        count: Int,
    ) = search(
        firstOffset = firstOffset, count = count,
        searchToPast = true, startOverride = null, datetimeOverride = null,
    )

    fun search(
        firstOffset: Int,
        count: Int,
        startOverride: StopName,
        datetimeOverride: LocalDateTime,
    ) = search(
        firstOffset = firstOffset, count = count,
        searchToPast = false, startOverride = startOverride, datetimeOverride = datetimeOverride,
    )

    fun search(
        firstOffset: Int,
        count: Int,
        searchToPast: Boolean,
        startOverride: StopName?,
        datetimeOverride: LocalDateTime?,
    ) = flow {
        logTime("ČAS: Nalezeno všech $count spojení") {
            repeat(count) { i ->
                val skip = i + firstOffset
                logTime("ČAS: Spojení č. $skip nalezeno") {
                    val connection = find(
                        offset = skip,
                        searchToPast = searchToPast,
                        start = startOverride ?: start,
                        destination = destination,
                        datetime = datetimeOverride ?: datetime,
                    )
                    emit(connection)
                }
            }
        }
    }

    private fun find(
        offset: Int,
        searchToPast: Boolean,
        start: StopName,
        destination: StopName,
        datetime: LocalDateTime,
    ): Connection? {
        val searchTable = searchTroughGraph(
            start = start,
            destination = destination,
            datetime = datetime,
            offset = offset,
            searchToPast = searchToPast,
        )

        if (searchTable[destination]!!.cameFrom == null) {
            return null
        }

        val connection = searchTable.flattenTable(
            destination = destination,
            startStop = start
        )

        val joint = connection.joinSameBus().log("CONNECTION before optimization") { joinToString() }

        val extended = joint.extendBusesFromEnd().log("CONNECTION") { joinToString() }

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
                departurePlatform = first.departurePlatform,
                bus = bus,
                date = first.date,
                vehicleType = first.vehicleType,
                arrival = last.arrival,
                arrivalIndexOnBus = last.arrivalIndexOnBus,
                arrivalPlatform = last.arrivalPlatform,
                endStop = last.endStop,
            )
        }

    private fun searchTroughGraph(
        start: StopName,
        destination: StopName,
        datetime: LocalDateTime,
        offset: Int,
        searchToPast: Boolean,
    ): SearchTable {
        val searchTable: MutableSearchTable = (mapOf(
            start to SearchTableRow(
                cameFrom = null,
                arrival = datetime.time,
                arrivalIndexOnBus = -1,
                arrivalPlatform = null,
                lastDeparture = null,
                lastDeparturePlatform = null,
                lastBus = null,
                vehicleType = null,
                date = datetime.date,
//            transfers = 0,
            ),
        ) + (allStops - start).associateWith {
            SearchTableRow(
                cameFrom = null,
                arrival = datetime.time,
                arrivalIndexOnBus = -1,
                arrivalPlatform = null,
                lastDeparture = null,
                lastDeparturePlatform = null,
                lastBus = null,
                vehicleType = null,
                date = datetime.date + 100.days
//            transfers = Int.MAX_VALUE,
            )
        }).toMutableMap()

        val queue = PriorityQueue<Pair<StopName, LocalDateTime>>(compareBy { it.second })
        queue.offer(start to datetime)

        while (!queue.isEmpty()) {
            val (thisStop) = queue.poll()!!
            if (thisStop == destination) continue

            val row = searchTable[thisStop]!!
            val destinationRow = searchTable[destination]!!
            val currentResult = destinationRow.arrival.atDate(destinationRow.date)

            val allOptions = strippedDownGraph[thisStop.log("Current")]!!

            val willSearchToPast = thisStop == start && searchToPast

            val yesterday by lazy { allOptions.map { it to row.date - 1.days } }
            val today by lazy { allOptions.map { it to row.date } }
            val tomorrow by lazy { allOptions.map { it to row.date + 1.days } }
            val datedOptions = if (willSearchToPast) yesterday + today else today + tomorrow

            val runningOptions = datedOptions
                .filter { (it, date) ->
                    val transfer = if (it.departurePlatform == row.arrivalPlatform) 0.seconds else 60.seconds
                    val departure = it.departure.atDate(date) - transfer
                    val arrival = row.arrival.atDate(row.date)
                    val runsToday = it.to in allStops && runsAt[it.bus]!!(date)
                    if (willSearchToPast) departure < arrival && runsToday
                    else arrival <= departure && runsToday
                }

            val noTransfer = if (row.lastBus != null) runningOptions.find { (edge) ->
                edge.bus == row.lastBus && edge.departureIndexOnBus == row.arrivalIndexOnBus
            } else null

            val offsetOptions =
                (if (willSearchToPast) runningOptions.dropLast(offset)
                else runningOptions.drop(if (thisStop == start) offset else 0))
                    .log("Current", thisStop, "Next options") { map { it.first } }

            offsetOptions
                .groupBy { it.first.to }
                .forEach { (to, options) ->
                    val next = noTransfer?.takeIf { it.first.to == to }
                        ?: if (willSearchToPast) options.lastOrNull() else options.firstOrNull()

                    next
                        ?.takeIf { (edge, date) ->
                            val targetRow = searchTable[to]!!
                            val current = targetRow.arrival.atDate(targetRow.date)
                            val new = edge.arrival.log(
                                "Current", row.arrival, thisStop, edge.departure, "Next", to,
                                "Using", edge.bus, "No transfer", noTransfer?.first?.bus,
                                "Best time", current, "New time"
                            ).atDate(date)
                            new < current && new < currentResult
                        }
                        ?.let { (edge, date) ->
                            searchTable[to] = SearchTableRow(
                                cameFrom = thisStop,
                                arrival = edge.arrival,
                                arrivalIndexOnBus = edge.arrivalIndexOnBus,
                                arrivalPlatform = edge.arrivalPlatform,
                                lastBus = edge.bus,
                                vehicleType = edge.vehicleType,
                                date = date,
                                lastDeparture = edge.departure,
                                lastDeparturePlatform = edge.departurePlatform,
//                            transfers = if (noTransfer == null) row.transfers + 1 else row.transfers
                            )
                            queue.offer(to to edge.arrival.atDate(date))
                        }
                }
        }

        return searchTable.log { entries.filter { it.value.lastBus != null }.joinToString("\n") }
    }

    @OptIn(ExperimentalTime::class)
    private fun Connection.extendBusesFromEnd(): Connection {
        val newParts = toMutableList()
        var currentIndex = newParts.size

        while (--currentIndex > 0) {
            val thisPart = newParts[currentIndex]
            val previousPart = newParts[currentIndex - 1]

            val busStops = stops.getValue(thisPart.bus)
            val stopsBeforeThisPart = busStops.slice(0..<thisPart.departureIndexOnBus)

            val lastPartDepartureOnThisBus = stopsBeforeThisPart.withIndex()
                .findLast { it.value.name == previousPart.startStop && it.value.time >= previousPart.departure }

            if (lastPartDepartureOnThisBus == null)
                continue

            newParts[currentIndex] = thisPart.copy(
                startStop = lastPartDepartureOnThisBus.value.name,
                departure = lastPartDepartureOnThisBus.value.time,
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
                departurePlatform = row.lastDeparturePlatform,
                bus = row.lastBus!!,
                vehicleType = row.vehicleType,
                arrival = row.arrival,
                arrivalIndexOnBus = row.arrivalIndexOnBus,
                arrivalPlatform = row.arrivalPlatform,
                endStop = currentStop,
                date = row.date,
            )

            currentStop = row.cameFrom
        } while (currentStop != startStop)

        return reversedResult.reversed()
    }
}

class DirectConnectionSearcher private constructor(
    private val datetime: LocalDateTime,
    private val connections: Sequence<ConnectionPart>,
    logger: Logger,
) : ConnectionSearcher, Logger by logger {
    companion object : ConnectionSearcher.Companion {
        context(logger: Logger)
        override suspend operator fun invoke(
            start: StopName,
            destination: StopName,
            datetime: LocalDateTime,
            repo: SpojeRepository,
        ) = coroutineScope {
            datetime.log("Vyhledávám přímo", start, destination)

            val stops = repo.stops(datetime.date)
            val runsAtA = repo.connsRunAt(datetime.date)

            val all = async {
                stops.await().findAllConnections(start, destination, datetime.date)
            }

            val dated = async {
                all.await().getDatedConnections(runsAtA.await())
            }

            DirectConnectionSearcher(datetime, dated.await(), logger)
        }

        private fun Sequence<ConnectionPart>.getDatedConnections(
            runsAt: Map<BusName, (LocalDate) -> Boolean>,
        ): Sequence<ConnectionPart> {
            val yesterday = map { it.copy(date = it.date - 1.days) }
            val tomorrow = map { it.copy(date = it.date + 1.days) }
            val datedOptions = yesterday + this + tomorrow

            return datedOptions.filter {
                runsAt[it.bus]!!(it.date)
            }
        }

        private fun Map<GraphBus, List<StopNameTime>>.findAllConnections(
            start: StopName,
            destination: StopName,
            date: LocalDate,
        ) = entries
            .asSequence()
            .flatMap { (bus, stops) ->
                stops
                    .asSequence()
                    .withIndex()
                    .filter { (_, stop) ->
                        stop.name == start && StopType(stop.fixedCodes).canGetOn
                    }
                    .flatMap { (i, start) ->
                        stops
                            .asSequence()
                            .withIndex()
                            .drop(i + 1)
                            .filter { (_, stop) ->
                                stop.name == destination && StopType(stop.fixedCodes).canGetOff
                            }
                            .map { (j, end) ->
                                ConnectionPart(
                                    startStop = start.name,
                                    departure = start.departure!!,
                                    departureIndexOnBus = i,
                                    departurePlatform = start.platform,
                                    bus = bus.connName,
                                    vehicleType = bus.vehicleType,
                                    date = date,
                                    arrival = end.arrival!!,
                                    arrivalIndexOnBus = j,
                                    arrivalPlatform = end.platform,
                                    endStop = end.name,
                                )
                            }
                    }
            }
    }

    override fun search(firstOffset: Int, count: Int) =
        search(firstOffset, count, searchToPast = false)

    override fun searchBack(firstOffset: Int, count: Int) =
        search(firstOffset, count, searchToPast = true)

    fun search(
        firstOffset: Int,
        count: Int,
        searchToPast: Boolean,
    ) = flow {
        logTime("ČAS: Nalezeno všech $count spojení") {
            val filtered = connections
                .filter {
                    if (searchToPast) it.departure < datetime.time
                    else datetime.time <= it.departure
                }
            val connections = filtered
                .drop(firstOffset)
            repeat(count) { i ->
                emit(connections.elementAtOrNull(i)?.let(::listOf))
            }
        }
    }
}
