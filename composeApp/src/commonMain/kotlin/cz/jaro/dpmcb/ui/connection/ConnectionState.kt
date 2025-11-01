package cz.jaro.dpmcb.ui.connection

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class ConnectionState(
    val buses: List<Alternatives>,
    val length: Duration,
    val start: LocalDateTime,
)

data class Alternatives(
    val level: Int,
    val before: List<ConnectionBus?>,
    val now: ConnectionBus?,
    val after: List<ConnectionBus?>,
) {
    val all get() = before + listOf(now) + after
    val count = all.count { it != null }
}

data class ConnectionBus(
    val transferTime: Duration?,
    val bus: BusName,
    val line: ShortLine,
    val isTrolleybus: Boolean,
    val date: LocalDate,
    val startStop: StopName,
    val departure: LocalTime,
    val endStop: StopName,
    val arrival: LocalTime,
    val stopCount: Int,
    val direction: StopName,
    val length: Duration,
    val cursor: Int,
)
