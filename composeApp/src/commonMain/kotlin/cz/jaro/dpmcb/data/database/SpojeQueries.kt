package cz.jaro.dpmcb.data.database

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.generated.CodesOfBus
import cz.jaro.dpmcb.data.generated.Conn
import cz.jaro.dpmcb.data.generated.ConnStop
import cz.jaro.dpmcb.data.generated.CoreBus
import cz.jaro.dpmcb.data.generated.CoreBusInTimetable
import cz.jaro.dpmcb.data.generated.CoreBusOfSequence
import cz.jaro.dpmcb.data.generated.CoreDeparture
import cz.jaro.dpmcb.data.generated.EndStop
import cz.jaro.dpmcb.data.generated.Line
import cz.jaro.dpmcb.data.generated.SeqGroup
import cz.jaro.dpmcb.data.generated.SeqOfConn
import cz.jaro.dpmcb.data.generated.Stop
import cz.jaro.dpmcb.data.generated.TimeCode
import cz.jaro.dpmcb.data.realtions.connection.ConnectionBusInfo
import cz.jaro.dpmcb.data.realtions.connection.GraphBus
import cz.jaro.dpmcb.data.realtions.connection.StopNameTime
import cz.jaro.dpmcb.data.realtions.departures.StopOfDeparture
import cz.jaro.dpmcb.data.realtions.now_running.BusOfNowRunning
import cz.jaro.dpmcb.data.realtions.now_running.BusStartEnd
import cz.jaro.dpmcb.data.realtions.now_running.StopOfNowRunning

interface SpojeQueries {
    suspend fun findLongLine(line: ShortLine): LongLine

    suspend fun stopNames(tabs: List<Table>): List<String>

    suspend fun lineNumbers(tabs: List<Table>): List<ShortLine>

    suspend fun nextStops(line: LongLine, thisStop: String, tab: Table): List<String>

    suspend fun connStopsOnLineInDirection(stop: String, direction: Direction, tab: Table): List<CoreBusInTimetable>

    suspend fun endStops(stop: String, tab: Table): List<EndStop>

    suspend fun stopNamesOnConns(tabs: List<Table>): Map<BusName, List<StopName>>

    suspend fun stopNamesOfLine(tab: Table): List<String>

    suspend fun stopsOnConns(tabs: List<Table>): Map<GraphBus, List<StopNameTime>>

    suspend fun coreBus(connName: BusName, groups: List<SequenceGroup>, tab: Table): List<CoreBus>

    suspend fun codes(connName: BusName, tab: Table): List<CodesOfBus>

    suspend fun multiCodes(connNames: List<BusName>, tabs: List<Table>): Map<BusName, List<CodesOfBus>>

    suspend fun coreBusOfSequence(seq: SequenceCode, group: SequenceGroup): List<CoreBusOfSequence>

    suspend fun connsOfSeq(seq: SequenceCode, group: SequenceGroup, tabs: List<Table>): List<BusName>

    suspend fun seqOfConns(conns: Set<BusName>, groups: List<SequenceGroup>, tabs: List<Table>): Map<BusName, SequenceCode>

    suspend fun firstConnOfSeq(seq: SequenceCode, group: SequenceGroup, tabs: List<Table>): BusName

    suspend fun lastConnOfSeq(seq: SequenceCode, group: SequenceGroup, tabs: List<Table>): BusName

    suspend fun departures(stop: String, tabs: List<Table>, groups: List<SequenceGroup>): List<CoreDeparture>

    suspend fun findSequences(sequence1: String, sequence2: String, sequence3: String, sequence4: String, sequence5: String, sequence6: String): List<SequenceCode>

    suspend fun busesStartAndEnd(conns: List<BusName>, tabs: List<Table>): Map<BusName, BusStartEnd>

    suspend fun nowRunningBuses(connNames: List<BusName>, groups: List<SequenceGroup>, tabs: List<Table>): Map<BusOfNowRunning, List<StopOfNowRunning>>

    suspend fun connectionResultBuses(connNames: Set<BusName>, groups: List<SequenceGroup>, tabs: List<Table>): Map<ConnectionBusInfo, List<StopNameTime>>

    suspend fun codesOfSequences(tabs: List<Table>, groups: List<SequenceGroup>): Map<SequenceCode, Map<BusName, List<CodesOfBus>>>

    suspend fun oneDirectionLines(): List<LongLine>

    suspend fun connStops(connNames: List<BusName>, tabs: List<Table>): Map<BusName, List<StopOfDeparture>>

    suspend fun hasRestriction(tab: Table): Boolean

    suspend fun validity(tab: Table): cz.jaro.dpmcb.data.generated.Validity

    suspend fun doesConnExist(connName: BusName): BusName?

    suspend fun seqGroupsPerSequence(): Map<SequenceCode, List<SeqGroup>>

    suspend fun allLineNumbers(): List<LongLine>

    suspend fun allSequences(): List<SequenceCode>

    suspend fun connStops(): List<ConnStop> = emptyList()
    suspend fun stops(): List<Stop> = emptyList()
    suspend fun timeCodes(): List<TimeCode> = emptyList()
    suspend fun lines(): List<Line>
    suspend fun conns(): List<Conn> = emptyList()
    suspend fun seqOfConns(): List<SeqOfConn> = emptyList()
    suspend fun seqGroups(): List<SeqGroup> = emptyList()

    suspend fun insertConnStops(connStops: List<ConnStop>)
    suspend fun insertStops(stops: List<Stop>)
    suspend fun insertTimeCodes(timeCodes: List<TimeCode>)
    suspend fun insertLines(lines: List<Line>)
    suspend fun insertConns(conns: List<Conn>)
    suspend fun insertSeqOfConns(seqsOfBuses: List<SeqOfConn>)
    suspend fun insertSeqGroups(seqGroups: List<SeqGroup>)
}