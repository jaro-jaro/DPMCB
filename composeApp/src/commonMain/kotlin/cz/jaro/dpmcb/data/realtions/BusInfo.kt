package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.entities.types.VehicleType

data class BusInfo(
    val lowFloor: Boolean,
    val line: LongLine,
    val vehicleType: VehicleType,
    val connName: BusName,
    val sequence: SequenceCode?,
    val direction: Direction,
)