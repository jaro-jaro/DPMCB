package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.minus
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.plus
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.sek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import java.util.Calendar

@kotlinx.serialization.Serializable
data class Cas(val h: Int = 0, val min: Int = 0, val s: Int = 0) : Comparable<Cas> {

    companion object {
        infix fun Int.cas(o: Int) = Cas(this, o)

        fun List<Int>.toCas(): Cas {
            assert(size >= 2)
            assert(size <= 3)
            if (size == 2) return get(0) cas get(1)
            return Cas(get(0), get(1), get(2))
        }

        private fun Int.toCas() = if (this == 362340) nikdy else Cas(
            h = floorDiv(60 * 60),
            min = rem(60 * 60).floorDiv(60),
            s = rem(60 * 60).rem(60),
        )

        fun String?.toCas() = this
            ?.split(":")
            ?.map { it.toInt() }
            ?.toCas()
            ?: ted

        fun Trvani.toCas() = sek.toCas()

        val ted
            get() = Calendar.getInstance().let { it[Calendar.HOUR_OF_DAY] cas it[Calendar.MINUTE] }

        val presneTed = flow {
            while (currentCoroutineContext().isActive) {
                emit(Calendar.getInstance().let { Cas(it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE], it[Calendar.SECOND]) })
            }
        }
            .flowOn(Dispatchers.IO)
            .shareIn(MainScope(), SharingStarted.WhileSubscribed())

        val nikdy = Cas(99, 99)
    }

    override operator fun compareTo(other: Cas) = toInt().compareTo(other.toInt())
    operator fun minus(other: Cas) = toInt().minus(other.toInt()).sek
    operator fun minus(other: Trvani) = toInt().minus(other).toCas()
    operator fun plus(other: Trvani) = toInt().plus(other).toCas()
    operator fun div(other: Trvani) = toInt().div(other.sek)
    operator fun rem(other: Trvani) = toInt().rem(other.sek).toCas()

    fun toTrvani() = toInt().sek
    private fun toInt() = h * 60 * 60 + min * 60 + s
    override fun toString() = toString(drawSeconds = false)
    fun toString(drawSeconds: Boolean = false) = buildString {
        append(h)
        append(":")
        if ("$min".length <= 1) append("0")
        append(min)
        if (drawSeconds) {
            append(":")
            if ("$s".length <= 1) append("0")
            append(s)
        }
    }
}
