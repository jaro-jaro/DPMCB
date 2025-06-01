package cz.jaro.dpmcb.data.entities

expect class SeqOfConn(
    line: LongLine,
    connNumber: BusNumber,
    sequence: SequenceCode,
    group: SequenceGroup,
    orderInSequence: Int?,
) {
    val line: LongLine
    val connNumber: BusNumber
    val sequence: SequenceCode
    val group: SequenceGroup
    val orderInSequence: Int?
}