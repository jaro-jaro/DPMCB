package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Traction {
    @SerialName("electro")
    Electro,

    @SerialName("hybrid")
    Hybrid,

    @SerialName("partial")
    PartialTrolleybus,

    @SerialName("gas")
    Gas,

    @SerialName("diesel")
    Diesel,

    @SerialName("trolley")
    Trolleybus,

    @SerialName("other")
    Other,
}

fun Traction.isTypeOf(other: Traction) = when (other) {
    Traction.Electro -> this == Traction.Electro
    Traction.Diesel -> this == Traction.Diesel || this == Traction.Hybrid || this == Traction.Gas
    Traction.Trolleybus -> this == Traction.Trolleybus || this == Traction.PartialTrolleybus
    Traction.PartialTrolleybus -> this == Traction.PartialTrolleybus
    Traction.Gas -> this == Traction.Other
    else -> false
}