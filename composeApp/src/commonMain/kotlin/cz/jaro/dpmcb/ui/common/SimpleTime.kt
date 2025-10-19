package cz.jaro.dpmcb.ui.common

import cz.jaro.dpmcb.data.helperclasses.two
import kotlinx.datetime.LocalTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SimpleTime.Serializer::class)
data class SimpleTime(
    val h: Int,
    val min: Int,
) {
    object Serializer : KSerializer<SimpleTime> {
        override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor("SimpleTime", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder) = decoder.decodeString().split(":").map(String::toInt).let { SimpleTime(h = it[0], min = it[1]) }
        override fun serialize(encoder: Encoder, value: SimpleTime) = encoder.encodeString("${value.h.two()}:${value.min.two()}")
    }

    companion object {
        val invalid = SimpleTime(99, 99)
    }
    fun isInvalid() = this == invalid
}
fun SimpleTime?.orInvalid() = this ?: SimpleTime.invalid

fun SimpleTime.toLocalTime() = LocalTime(h, min)
fun LocalTime.toSimpleTime() = SimpleTime(hour, minute)