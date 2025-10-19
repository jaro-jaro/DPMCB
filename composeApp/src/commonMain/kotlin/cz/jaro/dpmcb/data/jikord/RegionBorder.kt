package cz.jaro.dpmcb.data.jikord

import kotlinx.serialization.Serializable

@Serializable
data class RegionBorder(
    val north: Double,
    val south: Double,
    val west: Double,
    val east: Double,
)
