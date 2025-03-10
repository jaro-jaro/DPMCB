package cz.jaro.dpmcb.data.entities

import androidx.room.Entity
import kotlinx.datetime.LocalDate

@Entity(primaryKeys = ["group"])
data class SeqGroup(
// Primary keys
    val group: SequenceGroup,
// Other
    val validFrom: LocalDate,
    val validTo: LocalDate,
)