package cz.jaro.dpmcb.data.helperclasses

import kotlinx.serialization.Serializable

@Serializable
data class SequenceType(
    val nominative: String,
    val genitive: String,
    val accusative: String,
    val order: Int,
)
