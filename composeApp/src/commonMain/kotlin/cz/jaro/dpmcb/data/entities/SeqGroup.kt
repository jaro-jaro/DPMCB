package cz.jaro.dpmcb.data.entities

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeqGroup(
// Primary keys
    @SerialName("groupNumber")
    val group: SequenceGroup,
// Other
    val validFrom: LocalDate,
    val validTo: LocalDate,
)