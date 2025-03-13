@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

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

context(StringBuilder) @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
operator fun String.unaryPlus(): StringBuilder = append(this)


inline fun <reified T> String.fromJson(json: Json = Json): T = json.decodeFromString<T>(this)
inline fun <reified T> T.toJson(json: Json = Json): String = json.encodeToString<T>(this)
inline fun <reified T> JsonElement.fromJsonElement(json: Json = Json): T = json.decodeFromJsonElement<T>(this)
inline fun <reified T> T.toJsonElement(json: Json = Json): JsonElement = json.encodeToJsonElement<T>(this)
