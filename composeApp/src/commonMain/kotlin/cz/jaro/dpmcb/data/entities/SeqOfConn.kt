package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity("", [], false, primaryKeys = ["line", "connNumber", "sequence", "group"], [], [])
@Serializable
data class SeqOfConn(
// Primary keys
    val line: LongLine,
    val connNumber: BusNumber,
    val sequence: SequenceCode,
    @SerialName("seqGroup")
    val group: SequenceGroup,
// Other
    val orderInSequence: Int?,
)