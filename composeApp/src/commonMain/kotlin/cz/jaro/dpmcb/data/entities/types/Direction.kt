package cz.jaro.dpmcb.data.entities.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Direction")
enum class Direction {
    @SerialName("P")
    POSITIVE,

    @SerialName("N")
    NEGATIVE;
}

fun Boolean.toDirection() = if (this) Direction.POSITIVE else Direction.NEGATIVE
