package cz.jaro.dpmcb.data.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.SqlDriver
import cz.jaro.dpmcb.Database
import cz.jaro.dpmcb.data.database.entities.Conn
import cz.jaro.dpmcb.data.database.entities.ConnStop
import cz.jaro.dpmcb.data.database.entities.Line
import cz.jaro.dpmcb.data.database.entities.SeqGroup
import cz.jaro.dpmcb.data.database.entities.SeqOfConn
import cz.jaro.dpmcb.data.database.entities.Stop
import cz.jaro.dpmcb.data.database.entities.TimeCode
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Direction
import cz.jaro.dpmcb.data.entities.LineType
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCodeType
import cz.jaro.dpmcb.data.entities.VehicleType
import cz.jaro.dpmcb.data.helperclasses.IO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun createDatabase(driver: SqlDriver) = Database(
    driver = driver,
    ConnAdapter = Conn.Adapter(
        tabAdapter = TableAdapter,
        connNumberAdapter = BusNumberAdapter,
        lineAdapter = LongLineAdapter,
        directionAdapter = DirectionAdapter,
        nameAdapter = BusNameAdapter,
    ),
    ConnStopAdapter = ConnStop.Adapter(
        tabAdapter = TableAdapter,
        connNumberAdapter = BusNumberAdapter,
        lineAdapter = LongLineAdapter,
        stopNumberAdapter = StopNumberAdapter,
        arrivalAdapter = LocalTimeAdapter,
        departureAdapter = LocalTimeAdapter,
        stopIndexOnLineAdapter = IntAdapter,
    ),
    LineAdapter = Line.Adapter(
        tabAdapter = TableAdapter,
        numberAdapter = LongLineAdapter,
        vehicleTypeAdapter = VehicleTypeAdapter,
        lineTypeAdapter = LineTypeAdapter,
        shortNumberAdapter = ShortLineAdapter,
        validFromAdapter = LocalDateAdapter,
        validToAdapter = LocalDateAdapter,
    ),
    SeqGroupAdapter = SeqGroup.Adapter(
        groupAdapter = SequenceGroupAdapter,
        validFromAdapter = LocalDateAdapter,
        validToAdapter = LocalDateAdapter,
    ),
    SeqOfConnAdapter = SeqOfConn.Adapter(
        lineAdapter = LongLineAdapter,
        connNumberAdapter = BusNumberAdapter,
        sequenceAdapter = SequenceCodeAdapter,
        groupAdapter = SequenceGroupAdapter,
    ),
    StopAdapter = Stop.Adapter(
        tabAdapter = TableAdapter,
        stopNumberAdapter = StopNumberAdapter,
        lineAdapter = LongLineAdapter,
    ),
    TimeCodeAdapter = TimeCode.Adapter(
        tabAdapter = TableAdapter,
        connNumberAdapter = BusNumberAdapter,
        lineAdapter = LongLineAdapter,
        typeAdapter = TimeCodeTypeAdapter,
        validFromAdapter = LocalDateAdapter,
        validToAdapter = LocalDateAdapter,
    ),
)

val IntAdapter = ColumnAdapter<Long, Int>(
    constructor = Long::toInt,
    getValue = Int::toLong,
)

fun <P, T : Any> ColumnAdapter(
    constructor: (P) -> T,
    getValue: (T) -> P,
) = object : ColumnAdapter<T, P> {
    override fun decode(databaseValue: P) = constructor(databaseValue)
    override fun encode(value: T) = getValue(value)
}

val BusNumberAdapter = IntAdapter
val StopNumberAdapter = IntAdapter
val SequenceGroupAdapter = IntAdapter

val TableAdapter = ColumnAdapter(::Table) { it.value }
val LongLineAdapter = ColumnAdapter(::LongLine) { it.value }
val ShortLineAdapter = ColumnAdapter(::ShortLine) { it.value }
val BusNameAdapter = ColumnAdapter(::BusName) { it.value }
val SequenceCodeAdapter = ColumnAdapter(::SequenceCode) { it.value }

val TimeCodeTypeAdapter = EnumColumnAdapter<TimeCodeType>()
val VehicleTypeAdapter = EnumColumnAdapter<VehicleType>()
val LineTypeAdapter = EnumColumnAdapter<LineType>()
val DirectionAdapter = EnumColumnAdapter<Direction>()

//val DirectionAdapter = ColumnAdapter<Long, Direction>(
//    constructor = { Direction.entries[it.toInt()] },
//    getValue = { it.ordinal.toLong() },
//)
val LocalDateAdapter = ColumnAdapter(
    constructor = LocalDate::parse,
    getValue = LocalDate::toString,
)
val LocalTimeAdapter = ColumnAdapter(
    constructor = LocalTime::parse,
    getValue = LocalTime::toString,
)


suspend fun <T : Any> Query<T>.list() = withContext(Dispatchers.IO) { awaitAsList() }
suspend fun <T : Any> Query<T>.one() = withContext(Dispatchers.IO) { awaitAsOne() }
suspend fun <T : Any> Query<T>.oneOrNull() = withContext(Dispatchers.IO) { awaitAsOneOrNull() }
fun <T : Any> Query<T>.listAsFlow() = asFlow().map { awaitAsList() }
fun <T : Any> Query<T>.oneAsFlow() = asFlow().map { awaitAsOne() }
fun <T : Any> Query<T>.oneOrNullAsFlow() = asFlow().map { awaitAsOneOrNull() }