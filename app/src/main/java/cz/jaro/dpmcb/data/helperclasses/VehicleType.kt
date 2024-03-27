package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VehicleType {
    @SerialName("A")
    AUTOBUS,
    @SerialName("V")
    VLAK,
    @SerialName("E")
    TRAMVAJ,
    @SerialName("L")
    LANOVA_DRAHA,
    @SerialName("M")
    METRO,
    @SerialName("P")
    PRIVOZ,
    @SerialName("T")
    TROLEJBUS,
    @SerialName("W")
    PESI,
}