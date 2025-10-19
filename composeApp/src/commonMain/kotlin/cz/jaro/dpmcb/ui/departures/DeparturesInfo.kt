package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.entities.ShortLine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class DeparturesInfo(
    val stop: String,
    val time: LocalTime?,
    val date: LocalDate,
    val scrollIndex: Int = 0,
    val lineFilter: ShortLine? = null,
    val stopFilter: String? = null,
    val compactMode: Boolean = false,
    val justDepartures: Boolean = false,
)