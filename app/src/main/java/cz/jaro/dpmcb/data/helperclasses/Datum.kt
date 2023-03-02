package cz.jaro.dpmcb.data.helperclasses

import java.util.Calendar

@kotlinx.serialization.Serializable
data class Datum(val den: Int, val mesic: Int, val rok: Int) : Comparable<Datum> {

    companion object {
        fun Int.toDatum() = Datum(this % 31, (this % 372) / 31, this / 372)

        fun String.toDatumDivne() = Datum(slice(0..1).toInt(), slice(2..3).toInt(), slice(4..7).toInt())

        val dnes
            get() = Calendar.getInstance().let { Datum(it[Calendar.DAY_OF_MONTH], it[Calendar.MONTH] + 1, it[Calendar.YEAR]) }

        class DatumRange(override val start: Datum, override val endInclusive: Datum) : ClosedRange<Datum> {
            override fun toString() = "$start..$endInclusive"
        }
    }

    override fun compareTo(other: Datum) = toInt().compareTo(other.toInt())
    operator fun minus(other: Datum) = toInt().minus(other.toInt())
    operator fun minus(other: Int) = toInt().minus(other).toDatum()
    operator fun plus(other: Int) = toInt().plus(other).toDatum()

    fun toInt() = rok * 372 + mesic * 31 + den
    fun toCalendar(): Calendar = Calendar.getInstance().apply { set(rok, mesic - 1, den) }
    override fun toString() = "$den. $mesic. $rok"
    operator fun rangeTo(second: Datum) = DatumRange(this, second)

}
