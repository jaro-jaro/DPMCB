package cz.jaro.dpmcb.data.realtions.favourites

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.datetime.LocalTime

data class StopOfFavourite(
    val time: LocalTime,
    val name: String,
    val connName: BusName,
)