package cz.jaro.dpmcb.data.helperclasses

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class LocalDateProgression(
    start: LocalDate,
    endInclusive: LocalDate,
    val step: Duration,
) : Iterable<LocalDate> {

    init {
        if (step <= Duration.ZERO) throw kotlin.IllegalArgumentException("Step must be positive.")
        if (start > endInclusive) throw kotlin.IllegalArgumentException("->")
    }

    val first = start

    val last = endInclusive

    override fun iterator() = object : Iterator<LocalDate> {
        private val finalElement: LocalDate = last
        private var hasNext: Boolean = first <= last
        private var next: LocalDate = if (hasNext) first else finalElement

        override fun hasNext(): Boolean = hasNext

        override fun next(): LocalDate {
            val value = next
            if (value == finalElement) {
                if (!hasNext) throw kotlin.NoSuchElementException()
                hasNext = false
            }
            else {
                next += step
            }
            return value
        }
    }
}

fun ClosedRange<LocalDate>.toProgression(step: Duration = 1.days) = LocalDateProgression(start, endInclusive, step)