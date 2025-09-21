package cz.jaro.dpmcb.data.realtions.favourites

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.types.VehicleType

data class Favourite(
    val lowFloor: Boolean,
    val line: LongLine,
    val vehicleType: VehicleType,
    val sequence: SequenceCode,
    val connName: BusName,
)