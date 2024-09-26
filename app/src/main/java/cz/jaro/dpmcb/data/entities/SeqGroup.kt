package cz.jaro.dpmcb.data.entities

import androidx.room.Embedded
import androidx.room.Entity

@Entity(primaryKeys = ["group"])
data class SeqGroup(
// Primary keys
    val group: SequenceGroup,
// Other
    @Embedded
    val validity: Validity,
)