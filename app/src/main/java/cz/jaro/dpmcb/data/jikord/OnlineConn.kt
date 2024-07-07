package cz.jaro.dpmcb.data.jikord

import cz.jaro.dpmcb.data.serializers.LocalTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
@SerialName("OnlineSpoj")
data class OnlineConn(
    val name: String,
    val delayMin: Float?,
    val vehicle: Int?,
    val lowFloor: Boolean?,
    @Serializable(with = LocalTimeSerializer::class) val nextStop: LocalTime?,
) {
    val line get() = name.split("/")[0].toInt() - 325_000
}

fun Transmitter.toOnlineConn(): OnlineConn {
    val cn = cn!!.split("|")
    return OnlineConn(
        name = "${cn[0]}/${cn[2]}",
        delayMin = cn[6].toIntOrNull()?.div(60F),
        vehicle = cn[11].toIntOrNull()?.minus(17_000),
        lowFloor = cn[10] == "1",
        nextStop = if (cn[7].isBlank()) null else LocalTime.of(cn[7].toInt() / 60, cn[7].toInt() % 60),
    )
}