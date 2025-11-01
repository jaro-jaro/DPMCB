package cz.jaro.dpmcb.data.realtions.connection

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.types.VehicleType

data class GraphBus(
    val connName: BusName,
    val vehicleType: VehicleType?,
)
