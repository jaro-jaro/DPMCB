package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity

@Entity("", [], false, primaryKeys = ["line", "connNumber", "sequence", "group"], [], [])
data class SeqOfConn(
// Primary keys
    val line: LongLine,
    val connNumber: BusNumber,
    val sequence: SequenceCode,
    val group: SequenceGroup,
// Other
    val orderInSequence: Int?,
)