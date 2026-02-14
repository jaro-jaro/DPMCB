package cz.jaro.dpmcb.ui.connection_results

import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.StopName
import cz.jaro.dpmcb.ui.connection.ConnectionDefinition
import cz.jaro.dpmcb.ui.connection_search.Favourite
import cz.jaro.dpmcb.ui.connection_search.Relations
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class ConnectionResultState(
    val relations: Relations,
    val datetime: LocalDateTime,
    val results: List<ConnectionResult>,
    val loading: Boolean,
    val loadingPast: Boolean,
    val isFavourite: Boolean,
    val showAdd: Boolean,
    val showRemove: Boolean,
    val partOf: List<IndexedValue<Favourite>>,
    val other: List<IndexedValue<Favourite>>,
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
    val line: LongLine,
    val isTrolleybus: Boolean,
    val transferTime: Duration?,
    val transferTight: Boolean,
    val transferLong: Boolean,
    val length: Duration,
)