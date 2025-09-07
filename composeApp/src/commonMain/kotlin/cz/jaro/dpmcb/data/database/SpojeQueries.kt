package cz.jaro.dpmcb.data.database

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SeqOfConn
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.Validity
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.realtions.CoreBus
import cz.jaro.dpmcb.data.realtions.bus.CodesOfBus
import cz.jaro.dpmcb.data.realtions.departures.CoreDeparture
import cz.jaro.dpmcb.data.realtions.departures.StopOfDeparture
import cz.jaro.dpmcb.data.realtions.now_running.BusOfNowRunning
import cz.jaro.dpmcb.data.realtions.now_running.StopOfNowRunning
import cz.jaro.dpmcb.data.realtions.sequence.CoreBusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.TimeOfSequence
import cz.jaro.dpmcb.data.realtions.timetable.CoreBusInTimetable
import cz.jaro.dpmcb.data.realtions.timetable.EndStop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

interface SpojeQueries {

    suspend fun findLongLine(line: ShortLine): LongLine

    suspend fun stopNames(tabs: List<Table>): List<String>

    suspend fun lineNumbers(tabs: List<Table>): List<ShortLine>

    suspend fun nextStops(line: LongLine, thisStop: String, tab: Table): List<String>

    suspend fun connStopsOnLineInDirection(
        stop: String,
        direction: Direction,
        tab: Table,
    ): List<CoreBusInTimetable>

    suspend fun endStops(
        stop: String,
        tab: Table,
    ): List<EndStop>

    suspend fun stopNamesOfLine(line: LongLine, tab: Table): List<String>

    suspend fun coreBus(connName: BusName, groups: List<SequenceGroup>, tab: Table): List<CoreBus>

    suspend fun codes(connName: BusName, tab: Table): List<CodesOfBus>

    suspend fun coreBusOfSequence(seq: SequenceCode, group: SequenceGroup?): List<CoreBusOfSequence>

    suspend fun connsOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): List<BusName>

    suspend fun firstConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName

    suspend fun lastConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName

    suspend fun departures(
        stop: String,
        tabs: List<Table>,
    ): List<CoreDeparture>

    suspend fun findSequences(
        sequence1: String,
        sequence2: String,
        sequence3: String,
        sequence4: String,
        sequence5: String,
        sequence6: String,
    ): List<SequenceCode>

    suspend fun lastStopTimesOfConnsInSequences(
        todayRunningSequences: List<SequenceCode>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<SequenceCode, Map<BusName, LocalTime>>

    suspend fun nowRunningBuses(connNames: List<BusName>, groups: List<SequenceGroup>, tabs: List<Table>): Map<BusOfNowRunning, List<StopOfNowRunning>>

    suspend fun fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
        date: LocalDate,
        tabs: List<Table>,
        groups: List<SequenceGroup>,
    ): Map<TimeOfSequence, Map<BusName, List<CodesOfBus>>>

    suspend fun oneDirectionLines(): List<LongLine>

    suspend fun connStops(connNames: List<BusName>, tabs: List<Table>): Map<BusName, List<StopOfDeparture>>

    suspend fun hasRestriction(tab: Table): Boolean

    suspend fun validity(tab: Table): Validity

    suspend fun doesConnExist(connName: BusName): String?

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
}