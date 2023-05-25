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
    val lineType: String? = null,
    val vehicleType: String,
)
