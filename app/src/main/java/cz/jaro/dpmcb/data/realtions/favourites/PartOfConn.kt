package cz.jaro.dpmcb.data.realtions.favourites

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PartOfConn")
data class PartOfConn(
    val busName: String,
    val start: Int,
    val end: Int,
) {
    operator fun contains(index: Int) = index in iterator()

    operator fun iterator() = start..end

    companion object {
        @Suppress("FunctionName")
        fun Empty(busName: String) = PartOfConn(busName, -1, -1)
    }
}