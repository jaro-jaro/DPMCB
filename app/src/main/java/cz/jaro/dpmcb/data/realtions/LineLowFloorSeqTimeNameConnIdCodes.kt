package cz.jaro.dpmcb.data.realtions

import java.time.LocalDate
import java.time.LocalTime

data class LineLowFloorSeqTimeNameConnIdCodes(
    val lowFloor: Boolean,
    val line: Int,
    val sequence: String?,
    val fixedCodes: String,
    val time: LocalTime,
    val name: String,
    val connId: String,
    val runs: Boolean,
    val from: LocalDate,
    val to: LocalDate,
)