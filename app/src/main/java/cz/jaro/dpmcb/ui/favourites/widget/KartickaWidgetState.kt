package cz.jaro.dpmcb.ui.favourites.widget

import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.ShortLine
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class KartickaWidgetState(
    val spojId: BusName,
    val linka: ShortLine,
    val vychoziZastavka: String,
    val vychoziZastavkaCas: LocalTime,
    val cilovaZastavka: String,
    val cilovaZastavkaCas: LocalTime,
)