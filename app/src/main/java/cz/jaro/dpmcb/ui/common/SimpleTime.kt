package cz.jaro.dpmcb.ui.common

import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class SimpleTime(
    val h: Int,
    val min: Int,
) {
    companion object {
        val invalid = SimpleTime(99, 99)
    }
}

fun SimpleTime.toLocalTime() = LocalTime.of(h, min)!!
fun LocalTime.toSimpleTime() = SimpleTime(hour, minute)