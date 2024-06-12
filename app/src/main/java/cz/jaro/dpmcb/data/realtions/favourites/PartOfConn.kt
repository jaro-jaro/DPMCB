package cz.jaro.dpmcb.data.realtions.favourites

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PartOfConn")
data class PartOfConn(
    val busName: String,
    val start: Int,
    val end: Int,
)