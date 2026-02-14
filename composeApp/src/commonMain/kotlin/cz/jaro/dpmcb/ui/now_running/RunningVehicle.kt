package cz.jaro.dpmcb.ui.now_running

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine

data class RunningVehicle(
    val busName: BusName,
    val vehicle: RegistrationNumber?,
    val sequence: SequenceCode?,
    val lineNumber: LongLine,
    val destination: String,
)