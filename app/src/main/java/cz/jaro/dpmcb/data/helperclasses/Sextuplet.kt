package cz.jaro.dpmcb.data.helperclasses

import java.io.Serializable


/**
 * Represents six values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Sextuplet exhibits value semantics, i.e. two sextuplets are equal if all six components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @param E type of the fifth value.
 * @param F type of the sixth value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 * @property fifth Fifth value.
 * @property sixth Sixth value.
 */
data class Sextuplet<out A, out B, out C, out D, out E, out F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
) : Serializable {

    /**
     * Returns string representation of the [Sextuplet] including its [first], [second], [third], [fourth], [fifth] and [sixth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth)"
}

/**
 * Converts this sextuplet into a list.
 */
fun <T> Sextuplet<T, T, T, T, T, T>.toList(): List<T> = listOf(first, second, third, fourth, fifth, sixth)

