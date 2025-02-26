package cz.jaro.dpmcb.data.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LineType {
    @SerialName("A")
    MESTSKA,
    @SerialName("B")
    PRIMESTSKA,
    @SerialName("N")
    MEZINARODNI_NEVNITRO,
    @SerialName("P")
    MEZINARODNI_VNITRO,
    @SerialName("V")
    VNITROKRAJSKA,
    @SerialName("Z")
    MEZIKRAJSKA,
    @SerialName("D")
    DALKOVA,
}