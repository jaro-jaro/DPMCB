package cz.jaro.dpmcb.data.jikord

import kotlinx.serialization.Serializable

@Serializable
data class RouteStop(
    val ds: Nothing?,
    val d: Int,
    val tc: Int,
    val t: String,
    val n: String?,
    val s: Nothing?,
    val x: Double,
    val y: Double,
    val c: Int? = null,
    val sc: Int?,
    val sn: Long,
)