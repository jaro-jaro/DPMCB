package cz.jaro.dpmcb.ui.main

import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class SimpleTime(
    val h: Int,
    val min: Int,
)

fun SimpleTime.toLocalTime() = LocalTime.of(h, min)!!
fun LocalTime.toSimpleTime() = SimpleTime(hour, minute)