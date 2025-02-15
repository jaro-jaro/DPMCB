@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

fun Int.two() = toLastDigits(2)

fun Int.atLeastDigits(amount: Int) = toString().atLeastDigits(amount)
fun CharSequence.atLeastDigits(amount: Int) = "0" * (amount - length) + this
fun Int.toLastDigits(amount: Int) = toString().toLastDigits(amount)
fun CharSequence.toLastDigits(amount: Int) = atLeastDigits(amount).takeLast(amount)
operator fun CharSequence.times(times: Int) = buildString {
    if (times <= 0) return@buildString
    repeat(times) {
        append(this@times)
    }
}

context(StringBuilder) @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
operator fun String.unaryPlus(): StringBuilder = append(this)
