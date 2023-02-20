package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.sek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
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
            h = div(60 * 60),
            min = rem(60 * 60).div(60),
            s = rem(60),
        )

        fun String?.toCas() = this
            ?.split(":")
            ?.map { it.toInt() }
            ?.toCas()
            ?: ted

        fun Trvani.toCas() = sek.toCas()

        val ted
            get() = Calendar.getInstance().let { Cas(it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE], it[Calendar.SECOND]) }

        val presneTed = flow {
            while (currentCoroutineContext().isActive) {
                delay(500)
                emit(Calendar.getInstance().let { Cas(it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE], it[Calendar.SECOND]) })
            }
        }
            .flowOn(Dispatchers.IO)
            .stateIn(MainScope(), SharingStarted.WhileSubscribed(), ted)

        val nikdy = Cas(99, 99)
    }

    override operator fun compareTo(other: Cas) = toInt().compareTo(other.toInt())
    operator fun minus(other: Cas) = toInt().minus(other.toInt()).sek
    operator fun minus(other: Trvani) = toTrvani().minus(other).toCas()
    operator fun plus(other: Trvani) = toTrvani().plus(other).toCas()
    operator fun div(other: Trvani) = toTrvani().div(other)
    operator fun rem(other: Trvani) = toTrvani().rem(other).toCas()

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
