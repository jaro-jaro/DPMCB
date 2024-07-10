package cz.jaro.dpmcb.data.realtions.favourites

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine

data class Favourite(
    val lowFloor: Boolean,
    val line: ShortLine,
    val connName: BusName,
)