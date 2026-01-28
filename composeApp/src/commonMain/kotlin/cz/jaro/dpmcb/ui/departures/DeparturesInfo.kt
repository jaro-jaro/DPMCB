package cz.jaro.dpmcb.ui.departures

import cz.jaro.dpmcb.data.entities.Platform
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class DeparturesInfo(
    val stop: String,
    val setTime: LocalTime?,
    val date: LocalDate,
    val scrollIndex: Int = 0,
    val lineFilter: ShortLine? = null,
    val stopFilter: StopName? = null,
    val platformFilter: Platform? = null,
    val compactMode: Boolean = false,
    val justDepartures: Boolean = false,
    val usedTime: LocalTime?,
)