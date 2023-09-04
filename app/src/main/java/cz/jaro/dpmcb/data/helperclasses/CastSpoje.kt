package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.Serializable

@Serializable
data class CastSpoje(
    val spojId: String,
    val start: Int,
    val end: Int,
)