package cz.jaro.dpmcb.data.realtions.connection

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.types.VehicleType

data class ConnectionBusInfo(
    val connName: BusName,
    val vehicleType: VehicleType,
    val sequence: SequenceCode,
)