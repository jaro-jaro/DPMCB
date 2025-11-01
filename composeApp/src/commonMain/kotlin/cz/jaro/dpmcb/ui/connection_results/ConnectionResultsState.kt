package cz.jaro.dpmcb.ui.connection_results

import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.ui.connection.ConnectionDefinition
import cz.jaro.dpmcb.ui.connection_search.SearchSettings
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class ConnectionResultState(
    val settings: SearchSettings,
    val results: List<ConnectionResult>,
    val loading: Boolean,
    val loadingPast: Boolean,
)

@Serializable
data class ConnectionResult(
    val parts: List<ConnectionResultBus>,
    val def: ConnectionDefinition,
    val length: Duration,
    val startStop: StopName,
    val departure: LocalDateTime,
    val endStop: StopName,
    val arrival: LocalDateTime,
    val transfers: List<StopName>,
)

@Serializable
data class ConnectionResultBus(
    val line: ShortLine,
    val isTrolleybus: Boolean,
    val transferTime: Duration?,
    val length: Duration,
)