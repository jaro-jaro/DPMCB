package cz.jaro.dpmcb.ui.connection

import cz.jaro.dpmcb.data.Connection
import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.serializer

//@Serializable(with = ConnectionPartDefinitionSerializer::class)
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

typealias ConnectionDefinition =/*
        @Serializable(with = ConnectionDefinitionSerializer::class) */List<ConnectionPartDefinition>

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class ConnectionDefinitionSerializer : KSerializer<ConnectionDefinition> {
    override val descriptor =
        buildSerialDescriptor("ConnectionDefinition", StructureKind.LIST) {}

    override fun serialize(encoder: Encoder, value: ConnectionDefinition) =
        encoder.encodeCollection(descriptor, value.size * 4) {
            value.forEachIndexed { i, part ->
                encodeSerializableElement(descriptor, 4 * i, serializer(), part.busName)
                encodeSerializableElement(descriptor, 4 * i + 1, serializer(), part.date)
                encodeIntElement(descriptor, 4 * i + 2, part.start)
                encodeIntElement(descriptor, 4 * i + 3, part.end)
            }
        }

    override fun deserialize(decoder: Decoder) =
        decoder.decodeStructure(descriptor) {
            val size = decodeCollectionSize(descriptor)
            List(size / 4) { i ->
                ConnectionPartDefinition(
                    busName = decodeSerializableElement(descriptor, 4 * i, serializer()),
                    date = decodeSerializableElement(descriptor, 4 * i + 1, serializer()),
                    start = decodeIntElement(descriptor, 4 * i + 2),
                    end = decodeIntElement(descriptor, 4 * i + 3),
                )
            }
        }
}
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class ConnectionPartDefinitionSerializer : KSerializer<ConnectionPartDefinition> {
    override val descriptor =
        buildSerialDescriptor("ConnectionPartDefinition", StructureKind.LIST) {}

    override fun serialize(encoder: Encoder, value: ConnectionPartDefinition) =
        encoder.encodeCollection(descriptor, 4) {
            encodeSerializableElement(descriptor, 0, serializer(), value.busName)
            encodeSerializableElement(descriptor, 1, serializer(), value.date)
            encodeIntElement(descriptor, 2, value.start)
            encodeIntElement(descriptor, 3, value.end)
        }

    override fun deserialize(decoder: Decoder) =
        decoder.decodeStructure(descriptor) {
            ConnectionPartDefinition(
                busName = decodeSerializableElement(descriptor, 0, serializer()),
                date = decodeSerializableElement(descriptor, 1, serializer()),
                start = decodeIntElement(descriptor, 2),
                end = decodeIntElement(descriptor, 3),
            )
        }
}

fun Connection.toConnectionDefinition(): ConnectionDefinition = map {
    ConnectionPartDefinition(
        it.bus, it.departure.date, it.departureIndexOnBus, it.arrivalIndexOnBus
    )
}

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