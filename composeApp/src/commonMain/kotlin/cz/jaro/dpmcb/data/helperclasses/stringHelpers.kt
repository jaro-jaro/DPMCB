@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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