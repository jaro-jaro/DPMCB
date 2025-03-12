package cz.jaro.dpmcb.data.database

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.Validity
import cz.jaro.dpmcb.data.realtions.CoreBus
import cz.jaro.dpmcb.data.realtions.bus.CodesOfBus
import cz.jaro.dpmcb.data.realtions.departures.CoreDeparture
import cz.jaro.dpmcb.data.realtions.departures.StopOfDeparture
import cz.jaro.dpmcb.data.realtions.now_running.BusOfNowRunning
import cz.jaro.dpmcb.data.realtions.now_running.StopOfNowRunning
import cz.jaro.dpmcb.data.realtions.sequence.CoreBusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.TimeOfSequence
import cz.jaro.dpmcb.data.realtions.timetable.CoreBusInTimetable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class SupabaseDataSource(
    private val supabase: SupabaseClient,
) : SpojeDataSource {
    override val needsToDownloadData = false

    override suspend fun findLongLine(line: ShortLine): LongLine =
        supabase.postgrest.rpc("findLongLine", mapOf("line" to line)).decodeSingle()

    override suspend fun stopNames(tabs: List<Table>): List<String> {
        return supabase.postgrest.rpc("stopNames", mapOf("tabs" to tabs)).decodeList()
    }

    override suspend fun lineNumbers(tabs: List<Table>): List<ShortLine> {
        return supabase.postgrest.rpc("lineNumbers", mapOf("tabs" to tabs)).decodeList()
    }

    override suspend fun allLineNumbers(): List<LongLine> {
        return supabase.postgrest.rpc("allLineNumbers").decodeList()
    }

    override suspend fun nextStops(
        line: LongLine,
        thisStop: String,
        tab: Table,
    ): List<String> {
        return supabase.postgrest.rpc("findNextStops", mapOf("line" to line, "thisStop" to thisStop, "tab" to tab)).decodeList()
    }

    override suspend fun connStopsOnLineWithNextStopAtDate(
        stop: String,
        nextStop: String,
        date: LocalDate,
        tab: Table,
    ): List<CoreBusInTimetable> {
        return supabase.postgrest.rpc("findConnStopsOnLineWithNextStopAtDate", mapOf("stop" to stop, "nextStop" to nextStop, "date" to date, "tab" to tab)).decodeList()
    }

    override suspend fun stopNamesOfLine(line: LongLine, tab: Table): List<String> {
        return supabase.postgrest.rpc("findStopNamesOfLine", mapOf("line" to line, "tab" to tab)).decodeList()
    }

    override suspend fun coreBus(
        connName: BusName,
        groups: List<SequenceGroup>,
        tab: Table,
    ): List<CoreBus> {
        return supabase.postgrest.rpc("findLongLine", mapOf("connName" to connName, "groups" to groups, "tab" to tab)).decodeList()
    }

    override suspend fun codes(
        connName: BusName,
        tab: Table,
    ): List<CodesOfBus> {
        return supabase.postgrest.rpc("findCodes", mapOf("connName" to connName, "tab" to tab)).decodeList()
    }

    override suspend fun coreBusOfSequence(
        seq: SequenceCode,
        group: SequenceGroup?,
    ): List<CoreBusOfSequence> {
        return supabase.postgrest.rpc("findCoreBusOfSequence", mapOf("seq" to seq, "group" to group)).decodeList()
    }

    override suspend fun connsOfSeq(
        seq: SequenceCode,
        group: SequenceGroup?,
        tabs: List<Table>,
    ): List<BusName> {
        return supabase.postgrest.rpc("findConnsOfSeq", mapOf("seq" to seq, "group" to group, "tabs" to tabs)).decodeList()
    }

    override suspend fun firstConnOfSeq(
        seq: SequenceCode,
        group: SequenceGroup?,
        tabs: List<Table>,
    ): BusName {
        return supabase.postgrest.rpc("findFirstConnOfSeq", mapOf("seq" to seq, "group" to group, "tabs" to tabs)).decodeSingle()
    }

    override suspend fun lastConnOfSeq(
        seq: SequenceCode,
        group: SequenceGroup?,
        tabs: List<Table>,
    ): BusName {
        return supabase.postgrest.rpc("findLastConnOfSeq", mapOf("seq" to seq, "group" to group, "tabs" to tabs)).decodeSingle()
    }

    override suspend fun departures(
        stop: String,
        tabs: List<Table>,
    ): List<CoreDeparture> {
        return supabase.postgrest.rpc("findDepartures", mapOf("stop" to stop, "tabs" to tabs)).decodeList()
    }

    override suspend fun findSequences(
        sequence1: String,
        sequence2: String,
        sequence3: String,
        sequence4: String,
        sequence5: String,
        sequence6: String,
    ): List<SequenceCode> {
        return supabase.postgrest.rpc("findSequences", mapOf("sequence1" to sequence1, "sequence2" to sequence2, "sequence3" to sequence3, "sequence4" to sequence4, "sequence5" to sequence5, "sequence6" to sequence6)).decodeList()
    }

    override suspend fun lastStopTimesOfConnsInSequences(
        todayRunningSequences: List<SequenceCode>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<SequenceCode, Map<BusName, LocalTime>> {
        return supabase.postgrest.rpc("findLastStopTimesOfConnsInSequences", mapOf("todayRunningSequences" to todayRunningSequences, "groups" to groups, "tabs" to tabs)).decodeSingle()
    }

    override suspend fun nowRunningBuses(
        connNames: List<BusName>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<BusOfNowRunning, List<StopOfNowRunning>> {
        return supabase.postgrest.rpc("findNowRunningBuses", mapOf("connNames" to connNames, "groups" to groups, "tabs" to tabs)).decodeSingle()
    }

    override suspend fun fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
        date: LocalDate,
        tabs: List<Table>,
        groups: List<SequenceGroup>,
    ): Map<TimeOfSequence, Map<BusName, List<CodesOfBus>>> {
        return supabase.postgrest.rpc("findFixedCodesOfTodayRunningSequencesAccordingToTimeCodes", mapOf("date" to date, "tabs" to tabs, "groups" to groups)).decodeSingle()
    }

    override suspend fun oneDirectionLines(): List<LongLine> {
        return supabase.postgrest.rpc("findOneDirectionLines").decodeList()
    }

    override suspend fun connStops(
        connNames: List<BusName>,
        tabs: List<Table>,
    ): Map<BusName, List<StopOfDeparture>> {
        return supabase.postgrest.rpc("findConnStops", mapOf("connNames" to connNames, "tabs" to tabs)).decodeSingle()
    }

    override suspend fun connStops(): List<ConnStop> {
        return supabase.postgrest.rpc("findConnStops").decodeList()
    }

    override suspend fun hasRestriction(tab: Table): Boolean {
        return supabase.postgrest.rpc("hasRestriction", mapOf("tab" to tab)).decodeSingle()
    }

    override suspend fun validity(tab: Table): Validity {
        return supabase.postgrest.rpc("findValidity", mapOf("tab" to tab)).decodeSingle()
    }

    override suspend fun doesConnExist(connName: BusName): String? {
        return supabase.postgrest.rpc("doesConnExist", mapOf("connName" to connName)).decodeSingle()
    }

    override suspend fun lineTables(line: LongLine): List<Line> {
        return supabase.postgrest.rpc("findLineTables", mapOf("line" to line)).decodeList()
    }

    override suspend fun seqGroups(seq: SequenceCode): List<SeqGroup> {
        return supabase.postgrest.rpc("findSeqGroups", mapOf("seq" to seq)).decodeList()
    }

    override suspend fun seqGroups(): List<SeqGroup> {
        return supabase.postgrest.rpc("findSeqGroups").decodeList()
    }

    override suspend fun allSequences(): List<SequenceCode> {
        return supabase.postgrest.rpc("findAllSequences").decodeList()
    }
}