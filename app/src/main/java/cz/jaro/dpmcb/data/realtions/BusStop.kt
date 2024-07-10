package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import kotlinx.datetime.LocalTime

data class BusStop(
    val time: LocalTime,
    val name: String,
    val line: ShortLine,
    val nextStop: String?,
    val connName: BusName,
    val type: StopType,
)