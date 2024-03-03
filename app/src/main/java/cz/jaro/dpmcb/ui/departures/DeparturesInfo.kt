package cz.jaro.dpmcb.ui.departures

import java.time.LocalTime

data class DeparturesInfo(
    val time: LocalTime,
    val scrollIndex: Int = 0,
    val lineFilter: Int? = null,
    val stopFilter: String? = null,
    val compactMode: Boolean = false,
    val justDepartures: Boolean = false,
)