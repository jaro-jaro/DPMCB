package cz.jaro.dpmcb.data.naJihu

import kotlinx.serialization.Serializable

/**
 * ?
 *
 * @property fontChar ?
 * @property text ?
 */
@Serializable
data class FixedCode(
    val fontChar: String? = null,
    val text: String = "",
)
