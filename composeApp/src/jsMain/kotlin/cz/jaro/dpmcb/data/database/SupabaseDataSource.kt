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
import cz.jaro.dpmcb.data.helperclasses.fromJson
import cz.jaro.dpmcb.data.helperclasses.fromJsonElement
import cz.jaro.dpmcb.data.helperclasses.toJson
import cz.jaro.dpmcb.data.helperclasses.toJsonElement
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPath
import io.ktor.http.parametersOf
import io.ktor.http.takeFrom
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class SupabaseDataSource(
    private val supabase: SupabaseClient,
) : SpojeDataSource {
    private inline val <reified T> T.j get() = toJson()
    private inline val <reified T> List<T>.l get() = "{${joinToString(",") { it.toJson() }}}"
    private inline val <reified T> T.s get() = toJson().fromJson<String>()

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val supabaseUrl = supabase.supabaseHttpUrl
    val supabaseKey = supabase.supabaseKey

    val client = HttpClient(Js)

    @Suppress("UNCHECKED_CAST")
    private suspend fun query(function: String, params: Map<String, String> = emptyMap()) = function.lowercase().let { name ->
//        "Calling $name($params)".work()
        client.get(URLBuilder().takeFrom(supabaseUrl).apply {
            encodedPathSegments = "/rest/v1/rpc/$name".encodeURLPath().split("/")
            parameters.appendAll(parametersOf(params.mapValues { listOf(it.value) }.mapKeys { it.key.lowercase() }))
            parameters.append("apikey", supabaseKey)
        }.build())
            .let { PostgrestResult(it.bodyAsText(), it.headers, supabase.postgrest) }
//            .work { "$name returned ${decodeAs<JsonElement>()}" }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun select(table: String) = run {
//        "Querying $table".work()
        println(supabaseUrl)
        client.get(URLBuilder().takeFrom(supabaseUrl).apply {
            encodedPathSegments = "/rest/v1/$table".encodeURLPath().split("/")
            parameters.append("select", "*")
            parameters.append("apikey", supabaseKey)
        }.build())
            .let { PostgrestResult(it.bodyAsText(), it.headers, supabase.postgrest) }
//            .work { "$table returned ${decodeAs<JsonElement>()}" }
    }

    private inline fun <reified T : Any> insertAll(rows: List<T>) =
        rows.chunked(1000).map { suspend { insert(it) } }

    private suspend inline fun <reified T : Any> insert(rows: List<T>) {
        val modified = rows.map {
            it.toJsonElement(supabaseSerializer(), json)
        }
        val name = T::class.simpleName!!
        supabase.postgrest.from(name).upsert(modified)
    }

    private fun <T> PostgrestResult.decodeWith(deserializer: DeserializationStrategy<T>): T =
        data.fromJson(deserializer, json)

    private val PostgrestResult.jsonElement get() = decodeWith(JsonElement.serializer())

    private inline fun <reified T> PostgrestResult.decodeObject(): T =
        decodeWith(supabaseSerializer())

    private inline fun <reified T> PostgrestResult.decodeObjectList(): List<T> =
        decodeWith(ListSerializer(supabaseSerializer()))

    private inline fun <reified T> PostgrestResult.decodeSingleObject(): T =
        decodeObjectList<T>().single()

    private inline fun <reified T> PostgrestResult.decodeColumnFromTable(column: String): List<T> =
        decodeList<Map<String, T>>().map { it.getValue(column) }

    // ---

    override val needsToDownloadData = false

    override suspend fun findLongLine(line: ShortLine): LongLine =
        query("findLongLine", mapOf("line" to line.j)).decodeAs()

    override suspend fun stopNames(tabs: List<Table>): List<String> =
        query("stopNames", mapOf("tabs" to tabs.l)).decodeColumnFromTable("stopname")

    override suspend fun lineNumbers(tabs: List<Table>): List<ShortLine> =
        query("lineNumbers", mapOf("tabs" to tabs.l)).decodeColumnFromTable("shortnumber")

    override suspend fun allLineNumbers(): List<LongLine> =
        query("allLineNumbers").decodeColumnFromTable("number")

    override suspend fun nextStops(line: LongLine, thisStop: String, tab: Table): List<String> =
        query("nextStops", mapOf("lineA" to line.j, "thisStop" to thisStop.s, "tabA" to tab.j)).decodeColumnFromTable("stopname")

    override suspend fun connStopsOnLineInDirection(
        stop: String,
        direction: Direction,
        tab: Table,
    ): List<CoreBusInTimetable> =
        query(
            "connStopsOnLineInDirection",
            mapOf("stop" to stop.s, "direction" to direction.s, "tabA" to tab.s)
        ).decodeObjectList()

    override suspend fun endStops(
        stop: String,
        tab: Table,
    ): List<EndStop> =
        query(
            "endStops",
            mapOf("stop" to stop.s, "tabA" to tab.s)
        ).decodeObjectList()

    override suspend fun stopNamesOfLine(line: LongLine, tab: Table): List<String> =
        query("stopNamesOfLine", mapOf("lineA" to line.j, "tabA" to tab.s)).decodeColumnFromTable("stopname")

    override suspend fun coreBus(connName: BusName, groups: List<SequenceGroup>, tab: Table): List<CoreBus> =
        query("coreBus", mapOf("connName" to connName.s, "groups" to groups.l, "tabA" to tab.s)).decodeObjectList()

    override suspend fun codes(connName: BusName, tab: Table): List<CodesOfBus> =
        query("codes", mapOf("connName" to connName.s, "tabA" to tab.s)).decodeObjectList()

    override suspend fun coreBusOfSequence(seq: SequenceCode, group: SequenceGroup?): List<CoreBusOfSequence> =
        query("coreBusOfSequence", mapOf("seq" to seq.s, "seqgroup" to group.j)).decodeObjectList()

    override suspend fun connsOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): List<BusName> =
        query("connsOfSeq", mapOf("seq" to seq.s, "seqGroupA" to group.j, "tabs" to tabs.l)).decodeColumnFromTable("connname")

    override suspend fun seqOfConns(conns: Set<BusName>, groups: List<SequenceGroup>, tabs: List<Table>): List<BusName> =
        query("connsOfSeq", mapOf("seq" to seq.s, "seqGroupA" to group.j, "tabs" to tabs.l)).decodeColumnFromTable("connname")

    override suspend fun firstConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName =
        query("firstConnOfSeq", mapOf("seq" to seq.j, "group" to group.j, "tabs" to tabs.l)).decodeSingle()

    override suspend fun lastConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName =
        query("lastConnOfSeq", mapOf("seq" to seq.j, "group" to group.j, "tabs" to tabs.l)).decodeSingle()

    override suspend fun departures(stop: String, tabs: List<Table>, groups: List<SequenceGroup>): List<CoreDeparture> =
        query("departures", mapOf("stop" to stop, "tabs" to tabs.l)).decodeList()

    override suspend fun findSequences(
        sequence1: String, sequence2: String, sequence3: String,
        sequence4: String, sequence5: String, sequence6: String,
    ): List<SequenceCode> = query(
        "findSequences", mapOf(
            "sequence1" to sequence1.s, "sequence2" to sequence2.s, "sequence3" to sequence3.s,
            "sequence4" to sequence4.s, "sequence5" to sequence5.s, "sequence6" to sequence6.s,
        )
    ).decodeColumnFromTable("sequence")

    override suspend fun lastStopTimesOfConnsInSequences(
        todayRunningSequences: List<SequenceCode>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<SequenceCode, Map<BusName, LocalTime>> =
        query(
            "lastStopTimesOfConnsInSequences",
            mapOf("todayRunningSequences" to todayRunningSequences.l, "groups" to groups.l, "tabs" to tabs.l)
        )
            .decodeAs<JsonArray>()
            .groupBy { it.jsonObject.getValue("sequence").fromJsonElement<SequenceCode>() }
            .mapValues { it1 ->
                it1.value
                    .groupBy { it.jsonObject.getValue("connname").fromJsonElement<BusName>() }
                    .mapValues { it.value.single().jsonObject.getValue("time_").fromJsonElement<LocalTime>() }
            }

    override suspend fun nowRunningBuses(
        connNames: List<BusName>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<BusOfNowRunning, List<StopOfNowRunning>> =
        query("nowRunningBuses", mapOf("connNames" to connNames.l, "groups" to groups.l, "tabs" to tabs.l))
            .decodeAs<JsonArray>()
            .groupBy({ it.fromJsonElement(supabaseSerializer<BusOfNowRunning>(), json) }, { it.fromJsonElement<StopOfNowRunning>(json) })

    override suspend fun fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
        date: LocalDate, tabs: List<Table>, groups: List<SequenceGroup>,
    ): Map<TimeOfSequence, Map<BusName, List<CodesOfBus>>> =
        query("fixedCodesOfTodayRunningSequencesAccordingToTimeCodes", mapOf("date" to date.s, "tabs" to tabs.l, "groups" to groups.l))
            .decodeAs<JsonArray>()
            .groupBy { it.fromJsonElement<TimeOfSequence>(json) }
            .mapValues { it1 ->
                it1.value.groupBy(
                    { it.jsonObject.getValue("connname").fromJsonElement<BusName>() },
                    { it.fromJsonElement(supabaseSerializer<CodesOfBus>(), json) })
            }

    override suspend fun oneDirectionLines(): List<LongLine> =
        query("oneDirectionLines").decodeColumnFromTable("line")

    override suspend fun connStops(connNames: List<BusName>, tabs: List<Table>): Map<BusName, List<StopOfDeparture>> =
        connNames.chunked(500).flatMap { chunk ->
            query("connStops", mapOf("connNames" to chunk.l, "tabs" to tabs.l))
                .decodeAs<JsonArray>()
        }
            .groupBy({ it.jsonObject.getValue("connname").fromJsonElement<BusName>() }, { it.fromJsonElement<StopOfDeparture>(json) })

    override suspend fun connStops(): List<ConnStop> =
        query("connStops").decodeObjectList()

    override suspend fun hasRestriction(tab: Table): Boolean =
        query("hasRestriction", mapOf("tabA" to tab.s)).decodeAs()

    override suspend fun validity(tab: Table): Validity =
        query("validity", mapOf("table" to tab.s)).decodeSingleObject()

    override suspend fun doesConnExist(connName: BusName): String? =
        query("doesConnExist", mapOf("connName" to connName.s)).decodeAsOrNull()

    override suspend fun lines(): List<Line> =
        select("Line").decodeObjectList()

    override suspend fun seqGroupsPerSequence(): Map<SequenceCode, List<SeqGroup>> =
        query("seqGroups").jsonElement.jsonArray.map { it.jsonObject }
            .groupBy({ it.getValue("sequence").fromJsonElement() }, { it.fromJsonElement(supabaseSerializer<SeqGroup>(), json) })

    override suspend fun allSequences(): List<SequenceCode> =
        query("allSequences").decodeColumnFromTable("sequence")

    override fun insertConnStops(connStops: List<ConnStop>) = insertAll(connStops)
    override fun insertStops(stops: List<Stop>) = insertAll(stops)
    override fun insertTimeCodes(timeCodes: List<TimeCode>) = insertAll(timeCodes)
    override fun insertLines(lines: List<Line>) = insertAll(lines)
    override fun insertConns(conns: List<Conn>) = insertAll(conns)
    override fun insertSeqOfConns(seqsOfBuses: List<SeqOfConn>) = insertAll(seqsOfBuses)
    override fun insertSeqGroups(seqGroups: List<SeqGroup>) = insertAll(seqGroups)
}