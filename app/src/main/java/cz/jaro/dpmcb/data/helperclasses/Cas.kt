package cz.jaro.dpmcb.data.helperclasses

import java.util.Calendar

@kotlinx.serialization.Serializable
data class Cas(val h: Int, val min: Int) : Comparable<Cas> {

    companion object {
        infix fun Int.cas(o: Int) = Cas(this, o)

        fun List<Int>.toCas() = this[0] cas this[1]
        fun Int.toCas() = if (equals(6039)) nikdy else div(60) cas rem(60)
        fun String?.toCas() = this
            ?.split(":")
            ?.map { it.toInt() }
            ?.toCas()
            ?: ted

        val ted
            get() = Calendar.getInstance().let { it[Calendar.HOUR_OF_DAY] cas it[Calendar.MINUTE] }
        val nikdy = 99 cas 99
    }

    override operator fun compareTo(other: Cas) = toInt().compareTo(other.toInt())
    operator fun minus(other: Cas) = toInt().minus(other.toInt())
    operator fun minus(other: Int) = toInt().minus(other).toCas()
    operator fun plus(other: Int) = toInt().plus(other).toCas()


    fun toInt() = h * 60 + min
    override fun toString() = "$h:${if (min.toString().length <= 1) "0" else ""}$min"
}
