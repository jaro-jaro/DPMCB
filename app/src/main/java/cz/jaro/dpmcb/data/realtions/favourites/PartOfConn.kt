package cz.jaro.dpmcb.data.realtions.favourites

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PartOfConn")
data class PartOfConn(
    val busName: BusName,
    val start: Int,
    val end: Int,
) {
    operator fun contains(index: Int) = index in iterator()

    operator fun iterator() = start..end

    companion object {
        @Suppress("FunctionName")
        fun Empty(busName: BusName) = PartOfConn(busName, -1, -1)
    }
}