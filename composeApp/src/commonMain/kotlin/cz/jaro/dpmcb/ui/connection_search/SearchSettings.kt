package cz.jaro.dpmcb.ui.connection_search

import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.nowHere
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
data class SearchSettings(
    val start: StopName,
    val destination: StopName,
    val directOnly: Boolean = false,
    val showInefficientConnections: Boolean = false,
    val datetime: LocalDateTime = SystemClock.nowHere(),
) {
    val key get() = "$start -> $destination"
}