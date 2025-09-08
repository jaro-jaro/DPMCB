package cz.jaro.dpmcb.data.entities.types

import cz.jaro.dpmcb.data.helperclasses.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Direction")
@Keep
enum class Direction {
    @SerialName("positive")
    POSITIVE,

    @SerialName("negative")
    NEGATIVE;
}

fun Boolean.toDirection() = if (this) Direction.POSITIVE else Direction.NEGATIVE
