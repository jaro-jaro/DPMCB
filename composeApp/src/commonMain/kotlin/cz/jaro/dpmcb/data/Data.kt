package cz.jaro.dpmcb.data

import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.helperclasses.SequenceType
import cz.jaro.dpmcb.data.helperclasses.Traction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

class SequenceConnectionSerializer : KSerializer<Pair<SequenceCode, SequenceCode>> {
    private val serializer = ListSerializer(serializer<SequenceCode>())
    override val descriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: Pair<SequenceCode, SequenceCode>) =
        encoder.encodeSerializableValue(serializer, value.toList())

    override fun deserialize(decoder: Decoder) =
        decoder.decodeSerializableValue(serializer).let { it[0] to it[1] }
}

typealias DividedSequencesWithMultipleBuses = List<SequenceCode>
typealias LineTraction = Map<Traction, List<LongLine>>
typealias SequenceConnections = List<@Serializable(with = SequenceConnectionSerializer::class) Pair<SequenceCode, SequenceCode>>
typealias SequenceTypes = Map<Char, SequenceType>

@Serializable
data class DownloadedData(
    val version: Int = -1,
    val dividedSequencesWithMultipleBuses: DividedSequencesWithMultipleBuses = emptyList(),
    val linesTraction: LineTraction = emptyMap(),
    val sequenceConnections: SequenceConnections = emptyList(),
    val sequenceTypes: SequenceTypes = emptyMap(),
)
