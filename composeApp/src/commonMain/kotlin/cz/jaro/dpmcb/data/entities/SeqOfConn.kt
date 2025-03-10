package cz.jaro.dpmcb.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["line", "connNumber", "sequence", "group"])
data class SeqOfConn(
// Primary keys
    val line: LongLine,
    val connNumber: BusNumber,
    val sequence: SequenceCode,
    val group: SequenceGroup,
// Other
    val orderInSequence: Int?,
)