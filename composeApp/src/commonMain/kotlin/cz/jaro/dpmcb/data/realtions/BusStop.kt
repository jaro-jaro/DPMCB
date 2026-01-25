package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.types.Direction
import kotlinx.datetime.LocalTime

data class BusStop(
    val time: LocalTime,
    val arrival: LocalTime?,
    val name: String,
    val line: ShortLine,
    val connName: BusName,
    val direction: Direction,
    val type: StopType,
    val platform: Platform?,
)