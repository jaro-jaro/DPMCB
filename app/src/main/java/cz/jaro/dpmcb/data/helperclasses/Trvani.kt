package cz.jaro.dpmcb.data.helperclasses

@JvmInline
value class Trvani(val sek: Int) : Comparable<Trvani> {

    override fun compareTo(other: Trvani) = sek.compareTo(other.sek)

    companion object {
        val nekonecne = Trvani(Int.MAX_VALUE)

        val Double.sek get() = toInt().sek
        val Int.sek get() = Trvani(this)

        val Double.min get() = times(60).sek
        val Int.min get() = times(60).sek

        val Double.hod get() = times(60).min
        val Int.hod get() = times(60).min


        operator fun Int.times(o: Trvani) = times(o.sek).sek
        operator fun Double.times(o: Trvani) = times(o.sek).sek
        operator fun Int.div(o: Trvani) = div(o.sek).sek
        operator fun Double.div(o: Trvani) = div(o.sek).sek
        operator fun Int.rem(o: Trvani) = rem(o.sek).sek
        operator fun Double.rem(o: Trvani) = rem(o.sek).sek
        operator fun Int.minus(o: Trvani) = minus(o.sek).sek
        operator fun Double.minus(o: Trvani) = minus(o.sek).sek
        operator fun Int.plus(o: Trvani) = plus(o.sek).sek
        operator fun Double.plus(o: Trvani) = plus(o.sek).sek
        fun Int.floorDiv(o: Trvani) = floorDiv(o.sek).sek
    }

    operator fun times(o: Int) = sek.times(o).sek
    operator fun times(o: Double) = sek.times(o).sek
    operator fun div(o: Trvani) = sek.toDouble().div(o.sek)
    operator fun rem(o: Trvani) = sek.rem(o.sek).sek
    operator fun plus(o: Trvani) = sek.plus(o.sek).sek

    val min get() = sek / 60.0
    val hod get() = min / 60.0
}
