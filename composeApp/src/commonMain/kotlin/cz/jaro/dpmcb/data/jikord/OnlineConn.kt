package cz.jaro.dpmcb.data.jikord

import cz.jaro.dpmcb.data.GlobalSettingsDataSource
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.vehicleNumber
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("OnlineSpoj")
data class OnlineConn(
    val name: BusName,
    val delayMin: Float?,
    val vehicle: RegistrationNumber?,
    val lowFloor: Boolean?,
    val nextStop: LocalTime?,
) {
    val line get() = name.shortLine()
}

context(gs: GlobalSettingsDataSource)
fun Transmitter.toOnlineConn(): OnlineConn {
    val cn = cn!!.split("|")
    return OnlineConn(
        name = BusName("${cn[0]}/${cn[2]}"),
        delayMin = if (cn[3].isBlank()) null else cn[6].toIntOrNull()?.div(60F),
        vehicle = gs.vehicleNumber(cn[11]),
        lowFloor = cn[10] == "1",
        nextStop = if (cn[7].isBlank()) null else LocalTime(cn[7].toInt() / 60, cn[7].toInt() % 60),
    )
}