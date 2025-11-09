package cz.jaro.dpmcb.ui.connection

import cz.jaro.dpmcb.data.Connection
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.ui.main.parseDate
import cz.jaro.dpmcb.ui.main.serialize
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.days

@Serializable
@SerialName("ConnectionPartDefinition")
data class ConnectionPartDefinition(
    val busName: BusName,
    val date: LocalDate,
    val start: Int,
    val end: Int,
) {
    override fun toString() = "$busName ($start..$end)"
}

@Serializable(with = ConnectionDefinitionSerializer::class)
@JvmInline
value class ConnectionDefinition(val list: List<ConnectionPartDefinition>) : List<ConnectionPartDefinition> by list {
    override fun toString() = list.toString()
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class ConnectionDefinitionSerializer : KSerializer<ConnectionDefinition> {
    override val descriptor = PrimitiveSerialDescriptor("ConnectionDefinition", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ConnectionDefinition) {
        val initial = value.first().date
        encoder.encodeString(
            value.mapIndexed { i, part ->
                val offset = initial.daysUntil(part.date).takeUnless { it == 0 }?.let { "~+$it" }.orEmpty()
                val date = if (i == 0) "~${initial.serialize()}" else offset
                val bus = part.busName.value.split("/").joinToString("-")
                "$bus~${part.start}..${part.end}${date}"
            }.joinToString(",")
        )
    }

    override fun deserialize(decoder: Decoder) =
        decoder.decodeString().split(",").runningFold(
            null as Pair<ConnectionPartDefinition, LocalDate>?,
        ) { p, part ->
            val split = part.split("~")
            val bus = split[0]
            val busName = BusName(bus.split("-").joinToString("/"))
            val part = split[1]
            val (start, end) = part.split("..").map { it.toInt() }
            val offset = split.getOrElse(2) { "0" }
            val date = if (p == null) offset.parseDate() else p.second + offset.toInt().days
            ConnectionPartDefinition(busName, date, start, end) to date
        }.drop(1).map { it!!.first }.toConnectionDefinition()
}

fun List<ConnectionPartDefinition>.toConnectionDefinition() = ConnectionDefinition(this)
fun ConnectionDefinition.drop(n: Int) = (this as List<ConnectionPartDefinition>).drop(n).toConnectionDefinition()

@JvmName("connectionToConnectionDefinition")
fun Connection.toConnectionDefinition(): ConnectionDefinition = map {
    ConnectionPartDefinition(
        it.bus, it.departure.date, it.departureIndexOnBus, it.arrivalIndexOnBus
    )
}.toConnectionDefinition()

typealias AlternativesDefinition = List<TreeDefinition>
fun AlternativesDefinition(vararg items: TreeDefinition): AlternativesDefinition = items.toList()

fun AlternativesDefinition.divide(i: Int): Triple<AlternativesDefinition, TreeDefinition, AlternativesDefinition> =
    Triple(take(i), get(i), drop(i + 1))

operator fun AlternativesDefinition.get(coordinates: Coordinates): TreeDefinition {
    val first = coordinates.first()
    val rest = coordinates.drop(1)
    val tree = this[first]
    return if (rest.isEmpty()) tree else tree.next[rest]
}

data class TreeDefinition(
    val part: ConnectionPartDefinition,
    val next: AlternativesDefinition,
) {
    override fun toString() = "$part -> $next"
}