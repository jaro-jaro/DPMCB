package cz.jaro.dpmcb.data.naJihu

import kotlinx.serialization.Serializable

/**
 * Typ Spoje
 *
 * @property lineType ATV
 * @property vehicleType ?
 */
@Serializable
data class ServiceType(
    val lineType: String?,
    val vehicleType: String,
)
