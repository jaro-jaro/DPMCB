package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import kotlinx.datetime.LocalDate

@Entity("", [], false, primaryKeys = ["group"], [], [])
data class SeqGroup(
// Primary keys
    val group: SequenceGroup,
// Other
    val validFrom: LocalDate,
    val validTo: LocalDate,
)