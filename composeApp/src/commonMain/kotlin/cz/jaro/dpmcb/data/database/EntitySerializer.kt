package cz.jaro.dpmcb.data.database

import cz.jaro.dpmcb.data.helperclasses.fromJsonElement
import cz.jaro.dpmcb.data.helperclasses.toJsonElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> supabaseSerializer(): KSerializer<T> = with(serializer<T>().descriptor) {
    object : KSerializer<T> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
            repeat(elementsCount) { i ->
                element<String>(getElementName(i).lowercase())
            }
        }
        private val json = Json {
            encodeDefaults = true
        }

        override fun serialize(encoder: Encoder, value: T) = encoder.encodeStructure(descriptor) {
            val jsonValue = value.toJsonElement(json).jsonObject.mapValues { it.value.jsonPrimitive }
            repeat(elementsCount) { i ->
                encodeSerializableElement(descriptor, i, JsonPrimitive.serializer(), jsonValue.getValue(getElementName(i)))
            }
        }

        override fun deserialize(decoder: Decoder): T = JsonObject(decoder.decodeStructure(descriptor) {
            map(descriptor) { i ->
                getElementName(i) to decodeSerializableElement(descriptor, i, JsonPrimitive.serializer())
            }
        }.toMap()).fromJsonElement(json)
    }
}

fun <T> CompositeDecoder.map(descriptor: SerialDescriptor, transform: (Int) -> T) = buildList {
    while (true) {
        when (val index = decodeElementIndex(descriptor)) {
            CompositeDecoder.DECODE_DONE -> break
            else -> this += transform(index)
        }
    }
}