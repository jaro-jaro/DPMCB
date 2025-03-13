package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity("", [], false, primaryKeys = ["group"], [], [])
@Serializable
data class SeqGroup(
// Primary keys
    @SerialName("groupNumber")
    val group: SequenceGroup,
// Other
    val validFrom: LocalDate,
    val validTo: LocalDate,
)