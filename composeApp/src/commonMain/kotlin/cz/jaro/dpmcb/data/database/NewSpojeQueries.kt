package cz.jaro.dpmcb.data.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import cz.jaro.dpmcb.Database
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.generated.CodesOfBus
import cz.jaro.dpmcb.data.generated.Conn
import cz.jaro.dpmcb.data.generated.ConnStop
import cz.jaro.dpmcb.data.generated.Line
import cz.jaro.dpmcb.data.generated.SeqGroup
import cz.jaro.dpmcb.data.generated.SeqOfConn
import cz.jaro.dpmcb.data.generated.SpojeQueries
import cz.jaro.dpmcb.data.generated.Stop
import cz.jaro.dpmcb.data.generated.TimeCode
import cz.jaro.dpmcb.data.realtions.connection.ConnectionBusInfo
import cz.jaro.dpmcb.data.realtions.connection.GraphBus
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.data.realtions.departures.StopOfDeparture
import cz.jaro.dpmcb.data.realtions.now_running.BusOfNowRunning
import cz.jaro.dpmcb.data.realtions.now_running.BusStartEnd
import cz.jaro.dpmcb.data.realtions.now_running.StopOfNowRunning

suspend fun <T : Any, K, V> Query<T>.awaitGrouped(
    keySelector: (T) -> K, valueTransform: (T) -> V,
) = awaitAsList().groupBy(keySelector, valueTransform)

suspend fun <T : Any, K1, K2, V> Query<T>.awaitDoubleGrouped(
    key1Selector: (T) -> K1, key2Selector: (T) -> K2, valueTransform: (T) -> V,
) = awaitAsList()
    .groupBy(key1Selector).mapValues { it.value.groupBy(key2Selector, valueTransform) }

suspend fun <T : Any, K, V> Query<T>.awaitAssociated(
    keySelector: (T) -> K, valueTransform: (T) -> V,
) = awaitAsList().associate { keySelector(it) to valueTransform(it) }

class SpojeDataSourceImpl(
    private val driver: SqlDriver,
    private val sq: SpojeQueries,
) : SpojeDataSource {
    override val q = object : cz.jaro.dpmcb.data.database.SpojeQueries {
        override suspend fun coreBus(connName: BusName, groups: List<SequenceGroup>, tab: Table) =
            sq.coreBus(connName, tab, groups).awaitAsList()

        override suspend fun codes(connName: BusName, tab: Table) =
            sq.codesOfBus(connName, tab).awaitAsList()

        override suspend fun multiCodes(connNames: List<BusName>, tabs: List<Table>) =
            sq.multiCodes(connNames, tabs)
                .awaitGrouped({ it.connName }, { CodesOfBus(it.fixedCodes, it.type, it.validFrom, it.validTo) })

        override suspend fun coreBusOfSequence(seq: SequenceCode, group: SequenceGroup) =
            sq.coreBusOfSequence(group, seq).awaitAsList()

        override suspend fun findLongLine(line: ShortLine) =
            sq.findLongLine(line).awaitAsOne()

        override suspend fun stopNames(tabs: List<Table>) =
            sq.stopNames(tabs).awaitAsList()

        override suspend fun lineNumbers(tabs: List<Table>) =
            sq.lineNumbers(tabs).awaitAsList()

        override suspend fun nextStops(line: LongLine, thisStop: String, tab: Table) =
            sq.nextStops(thisStop, line, tab).awaitAsList()

        override suspend fun connStopsOnLineOnPlatformInDirection(stop: String, platform: Platform, direction: Direction, tab: Table) =
            sq.coreBusInTimetable(tab, stop, platform, direction).awaitAsList()

        override suspend fun platformsAndDirections(stop: String, tab: Table) =
            sq.platformAndDirection(tab, stop).awaitAsList()

        override suspend fun platformsOfStop(stop: String, tabs: List<Table>) =
            sq.platformOfStop(tabs, stop).awaitAsList()

        override suspend fun stopNamesOnConns(tabs: List<Table>) =
            sq.stopNamesOnConns(tabs).awaitGrouped({ it.connName }, { it.stopName })

        override suspend fun stopNamesOfLine(tab: Table) =
            sq.stopNamesOfLine(tab).awaitAsList()

        override suspend fun stopsOnConns(tabs: List<Table>) =
            sq.stopsOnConns(tabs).awaitGrouped(
                { GraphBus(it.connName, it.vehicleType) },
                { StopNameTime(it.name, it.departure, it.arrival, it.time!!, it.fixedCodes, it.platform) }
            )

        override suspend fun connsOfSeq(seq: SequenceCode, group: SequenceGroup, tabs: List<Table>) =
            sq.connsOfSeq(seq, group, tabs).awaitAsList()

        override suspend fun seqOfConns(conns: Set<BusName>, groups: List<SequenceGroup>, tabs: List<Table>) =
            sq.seqOfConns(conns, groups, tabs).awaitAssociated({ it.connName }, { it.sequence })

        override suspend fun firstConnOfSeq(seq: SequenceCode, group: SequenceGroup, tabs: List<Table>) =
            sq.firstConnOfSeq(seq, group, tabs).awaitAsOne()

        override suspend fun lastConnOfSeq(seq: SequenceCode, group: SequenceGroup, tabs: List<Table>) =
            sq.lastConnOfSeq(seq, group, tabs).awaitAsOne()

        override suspend fun departures(stop: String, tabs: List<Table>, groups: List<SequenceGroup>) =
            sq.coreDeparture(stop, tabs, groups).awaitAsList()

        override suspend fun findSequences(
            sequence1: String, sequence2: String, sequence3: String, sequence4: String, sequence5: String, sequence6: String,
        ) =
            sq.findSequences(sequence1, sequence2, sequence3, sequence4, sequence5, sequence6).awaitAsList()

        override suspend fun busesStartAndEnd(conns: List<BusName>, tabs: List<Table>) =
            sq.busesStartAndEnd(tabs, conns).awaitAssociated({ it.connName }, { BusStartEnd(it.start, it.end) })

        override suspend fun nowRunningBuses(connNames: List<BusName>, groups: List<SequenceGroup>, tabs: List<Table>) =
            sq.nowRunningBuses(connNames, groups, tabs).awaitGrouped(
                { BusOfNowRunning(it.connName, it.line, it.direction, it.sequence, it.tab) },
                { StopOfNowRunning(it.name, it.time!!) },
            )

        override suspend fun connectionResultBuses(connNames: Set<BusName>, groups: List<SequenceGroup>, tabs: List<Table>) =
            sq.connectionResultBuses(connNames, groups, tabs).awaitGrouped(
                { ConnectionBusInfo(it.connName, it.vehicleType, it.sequence) },
                { StopNameTime(it.name, it.departure, it.arrival, it.time!!, it.fixedCodes, it.platform) }
            )

        override suspend fun codesOfSequences(tabs: List<Table>, groups: List<SequenceGroup>) =
            sq.codesOfSequences(tabs, groups)
                .awaitDoubleGrouped({ it.sequence }, { it.connName }, { CodesOfBus(it.fixedCodes, it.type, it.validFrom, it.validTo) })

        override suspend fun oneDirectionLines() =
            sq.oneDirectionLines().awaitAsList()

        override suspend fun connStops(connNames: List<BusName>, tabs: List<Table>) =
            sq.connStops(connNames, tabs).awaitGrouped({ it.connName }, { StopOfDeparture(it.name, it.time!!, it.stopIndexOnLine) })

        override suspend fun hasRestriction(tab: Table) =
            sq.hasRestriction(tab).awaitAsOne()

        override suspend fun validity(tab: Table) =
            sq.validity(tab).awaitAsOne()

        override suspend fun doesConnExist(connName: BusName) =
            sq.doesConnExist(connName).awaitAsOneOrNull()

        override suspend fun seqGroupsPerSequence() =
            sq.seqGroupsPerSequence()
                .awaitGrouped({ it.sequence }, { SeqGroup(it.seqGroup, it.validFrom, it.validTo) })

        override suspend fun allLineNumbers() =
            sq.allLineNumbers().awaitAsList()

        override suspend fun allSequences() =
            sq.allSequences().awaitAsList()

        override suspend fun connStops() =
            sq.allConnStops().awaitAsList()

        override suspend fun stops() =
            sq.allStops().awaitAsList()

        override suspend fun timeCodes() =
            sq.allTimeCodes().awaitAsList()

        override suspend fun lines() =
            sq.allLines().awaitAsList()

        override suspend fun conns() =
            sq.allConns().awaitAsList()

        override suspend fun seqOfConns() =
            sq.allSeqOfConns().awaitAsList()

        override suspend fun seqGroups() =
            sq.allSeqGroups().awaitAsList()

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertConnStops(connStops: List<ConnStop>) =
            sq.transaction(false) {
                connStops.forEach { sq.insertConnStop(it) }
            }

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertStops(stops: List<Stop>) =
            sq.transaction(false) {
                stops.forEach { sq.insertStop(it) }
            }

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertTimeCodes(timeCodes: List<TimeCode>) =
            sq.transaction(false) {
                timeCodes.forEach { sq.insertTimeCode(it) }
            }

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertLines(lines: List<Line>) =
            sq.transaction(false) {
                lines.forEach { sq.insertLine(it) }
            }

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertConns(conns: List<Conn>) =
            sq.transaction(false) {
                conns.forEach { sq.insertConn(it) }
            }

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertSeqOfConns(seqsOfBuses: List<SeqOfConn>) =
            sq.transaction(false) {
                seqsOfBuses.forEach { sq.insertSeqOfConn(it) }
            }

        @Suppress("RETURN_VALUE_NOT_USED")
        override suspend fun insertSeqGroups(seqGroups: List<SeqGroup>) =
            sq.transaction(false) {
                seqGroups.forEach { sq.insertSeqGroup(it) }
            }
    }

    override suspend fun dropAllTables() {
        sq.dropAllTables().await()
    }

    override suspend fun createTables() {
        Database.Schema.create(driver).await()
    }

    override val needsToDownloadData = true
}