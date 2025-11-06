@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import androidx.compose.ui.text.AnnotatedString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer

fun Int.two() = toLastDigits(2)

fun Int.atLeastDigits(amount: Int) = toString().atLeastDigits(amount)
fun Long.atLeastDigits(amount: Int) = toString().atLeastDigits(amount)
fun CharSequence.atLeastDigits(amount: Int) = "0" * (amount - length) + this
fun Int.toLastDigits(amount: Int) = toString().toLastDigits(amount)
fun Long.toLastDigits(amount: Int) = toString().toLastDigits(amount)
fun CharSequence.toLastDigits(amount: Int) = atLeastDigits(amount).takeLast(amount)
operator fun CharSequence.times(times: Int) = buildString {
    if (times <= 0) return@buildString
    repeat(times) {
        append(this@times)
    }
}

context(builder: StringBuilder)
operator fun String.unaryPlus() = builder.append(this)

context(builder: AnnotatedString.Builder)
operator fun String.unaryPlus() = builder.append(this)

context(builder: AnnotatedString.Builder)
operator fun AnnotatedString.unaryPlus() = builder.append(this)


inline fun <reified T> String.fromJson(json: Json = Json): T = json.decodeFromString<T>(this)
fun <T> String.fromJson(deserializer: DeserializationStrategy<T>, json: Json = Json): T = json.decodeFromString<T>(deserializer, this) as T
inline fun <reified T> T.toJson(json: Json = Json): String = json.encodeToString<T>(serializer(), this)
fun <T> T.toJson(serializer: SerializationStrategy<T>, json: Json = Json): String = json.encodeToString<T>(serializer, this)

inline fun <reified T> JsonElement.fromJsonElement(json: Json = Json): T = json.decodeFromJsonElement<T>(this)
fun <T> JsonElement.fromJsonElement(deserializer: DeserializationStrategy<T>, json: Json = Json): T = json.decodeFromJsonElement<T>(deserializer, this)
inline fun <reified T> T.toJsonElement(json: Json = Json): JsonElement = json.encodeToJsonElement<T>(this)
fun <T> T.toJsonElement(serializer: SerializationStrategy<T>, json: Json = Json): JsonElement = json.encodeToJsonElement<T>(serializer, this)
