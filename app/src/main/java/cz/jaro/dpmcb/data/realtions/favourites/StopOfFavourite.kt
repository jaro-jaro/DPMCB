package cz.jaro.dpmcb.data.realtions.favourites

import java.time.LocalTime

data class StopOfFavourite(
    val time: LocalTime,
    val name: String,
    val connName: String,
)