package cz.jaro.dpmcb.data.entities

import kotlinx.datetime.LocalDate

expect class SeqGroup(
    group: SequenceGroup,
    validFrom: LocalDate,
    validTo: LocalDate,
) {
    val group: SequenceGroup
    val validFrom: LocalDate
    val validTo: LocalDate
}