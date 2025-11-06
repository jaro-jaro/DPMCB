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
): Iterable<Int> {
    val range get() = start..end

    operator fun contains(index: Int): Boolean = index in range

    override operator fun iterator() = range.iterator()
}

@Suppress("FunctionName")
fun PartOfConn.Companion.Empty(busName: BusName) = PartOfConn(busName, -1, -1)

fun PartOfConn.isEmpty() = start == -1 && end == -1
fun PartOfConn.isNotEmpty() = start != -1 || end != -1

fun PartOfConn?.orEmpty(busName: BusName) = this ?: PartOfConn.Empty(busName)