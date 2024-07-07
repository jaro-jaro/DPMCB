package cz.jaro.dpmcb.data.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalTime

class LocalTimeSerializer : KSerializer<LocalTime> {
    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.ofSecondOfDay(decoder.decodeLong())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeLong(value.toSecondOfDay().toLong())
    }
}

class NullableLocalTimeSerializer : KSerializer<LocalTime?> {
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalTime? {
        decoder.decodeNotNullMark()
        return LocalTime.ofSecondOfDay(decoder.decodeLong())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.LONG)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: LocalTime?) {
        when (val res = value?.toSecondOfDay()?.toLong()) {
            null -> encoder.encodeNull()
            else -> encoder.encodeLong(res)
        }
    }
}