package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CastSpoje")
data class PartOfConn(
    @SerialName("spojId") val busId: String,
    val start: Int,
    val end: Int,
)