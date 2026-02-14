package cz.jaro.dpmcb.data.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.adapter.primitive.ShortColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import cz.jaro.dpmcb.Database
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.generated.Conn
import cz.jaro.dpmcb.data.generated.ConnStop
import cz.jaro.dpmcb.data.generated.Line
import cz.jaro.dpmcb.data.generated.SeqGroup
import cz.jaro.dpmcb.data.generated.SeqOfConn
import cz.jaro.dpmcb.data.generated.Stop
import cz.jaro.dpmcb.data.generated.TimeCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ValueColumnAdapter<T : Any, S>(
    private val constructor: (S) -> T,
    private val getter: T.() -> S,
) : ColumnAdapter<T, S> {
    override fun decode(databaseValue: S): T = constructor(databaseValue)
    override fun encode(value: T): S = value.getter()
}

class AliasColumnAdapter<T : Any> : ColumnAdapter<T, T> {
    override fun decode(databaseValue: T): T = databaseValue
    override fun encode(value: T): T = value
}

class CombinedColumnAdapter<T : Any, I : Any, S>(
    private val first: ColumnAdapter<T, I>,
    private val second: ColumnAdapter<I, S>,
) : ColumnAdapter<T, S> {
    override fun decode(databaseValue: S): T = first.decode(second.decode(databaseValue))
    override fun encode(value: T): S = second.encode(first.encode(value))
}

infix fun <T : Any, I : Any, S> ColumnAdapter<T, I>.then(other: ColumnAdapter<I, S>) =
    CombinedColumnAdapter(this, other)

object LocalDateColumnAdapter : ColumnAdapter<LocalDate, String> {
    override fun decode(databaseValue: String) = LocalDate.parse(databaseValue)
    override fun encode(value: LocalDate) = value.toString()
}

object LocalTimeColumnAdapter : ColumnAdapter<LocalTime, String> {
    override fun decode(databaseValue: String) = LocalTime.parse(databaseValue)
    override fun encode(value: LocalTime) = value.toString()
}

object StopNameColumnAdapter : ColumnAdapter<StopName, String> {
    override fun decode(databaseValue: String) = StopName.deserialize(databaseValue)
    override fun encode(value: StopName) = value.serialize()
}

private val adapterTable = ValueColumnAdapter(::Table, Table::value)
private val adapterLongLine = ValueColumnAdapter(::LongLine, LongLine::value) then IntColumnAdapter
private val adapterShortLine = ValueColumnAdapter(::ShortLine, ShortLine::value) then IntColumnAdapter
private val adapterBusName = ValueColumnAdapter(::BusName, BusName::value)
private val adapterSequenceCode = ValueColumnAdapter(::SequenceCode, SequenceCode::value)

fun createDatabase(
    driver: SqlDriver
) = Database.invoke(
    driver = driver,
    ConnAdapter = Conn.Adapter(
        tabAdapter = adapterTable,
        connNumberAdapter = IntColumnAdapter,
        lineAdapter = adapterLongLine,
        directionAdapter = EnumColumnAdapter(),
        nameAdapter = adapterBusName,
    ),
    ConnStopAdapter = ConnStop.Adapter(
        tabAdapter = adapterTable,
        connNumberAdapter = IntColumnAdapter,
        stopIndexOnLineAdapter = IntColumnAdapter,
        lineAdapter = adapterLongLine,
        stopNumberAdapter = IntColumnAdapter,
        kmFromStartAdapter = IntColumnAdapter,
        arrivalAdapter = LocalTimeColumnAdapter,
        departureAdapter = LocalTimeColumnAdapter,
        platformAdapter = AliasColumnAdapter(),
    ),
    LineAdapter = Line.Adapter(
        tabAdapter = adapterTable,
        numberAdapter = adapterLongLine,
        vehicleTypeAdapter = EnumColumnAdapter(),
        lineTypeAdapter = EnumColumnAdapter(),
        validFromAdapter = LocalDateColumnAdapter,
        validToAdapter = LocalDateColumnAdapter,
        shortNumberAdapter = adapterShortLine,
    ),
    SeqGroupAdapter = SeqGroup.Adapter(
        seqGroupAdapter = IntColumnAdapter,
        validFromAdapter = LocalDateColumnAdapter,
        validToAdapter = LocalDateColumnAdapter,
    ),
    SeqOfConnAdapter = SeqOfConn.Adapter(
        lineAdapter = adapterLongLine,
        connNumberAdapter = IntColumnAdapter,
        sequenceAdapter = adapterSequenceCode,
        seqGroupAdapter = IntColumnAdapter,
        orderInSequenceAdapter = IntColumnAdapter,
    ),
    StopAdapter = Stop.Adapter(
        tabAdapter = adapterTable,
        stopNumberAdapter = IntColumnAdapter,
        lineAdapter = adapterLongLine,
        stopNameAdapter = StopNameColumnAdapter,
        fareZoneAdapter = IntColumnAdapter,
    ),
    TimeCodeAdapter = TimeCode.Adapter(
        tabAdapter = adapterTable,
        connNumberAdapter = IntColumnAdapter,
        codeAdapter = ShortColumnAdapter,
        termIndexAdapter = ShortColumnAdapter,
        lineAdapter = adapterLongLine,
        typeAdapter = EnumColumnAdapter(),
        validFromAdapter = LocalDateColumnAdapter,
        validToAdapter = LocalDateColumnAdapter,
    ),
)