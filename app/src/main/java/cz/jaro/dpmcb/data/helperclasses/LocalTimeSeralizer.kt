package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalTime

class LocalTimeSeralizer : KSerializer<LocalTime> {
    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.ofSecondOfDay(decoder.decodeLong())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeLong(value.toSecondOfDay().toLong())
    }
}