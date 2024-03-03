package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate

data class ConnIdSeqCodesTab(
    val sequence: String?,
    val runs: Boolean,
    val from: LocalDate,
    val to: LocalDate,
    val fixedCodes: String,
    val tab: String,
    val connId: String,
)
