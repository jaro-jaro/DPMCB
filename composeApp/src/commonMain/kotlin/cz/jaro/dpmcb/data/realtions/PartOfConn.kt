package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.entities.BusName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
@OptIn(ExperimentalContracts::class)
fun PartOfConn?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || isEmpty()
}

fun PartOfConn?.orEmpty(busName: BusName) = this ?: PartOfConn.Empty(busName)