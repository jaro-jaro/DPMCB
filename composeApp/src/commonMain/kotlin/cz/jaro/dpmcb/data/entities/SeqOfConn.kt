package cz.jaro.dpmcb.data.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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