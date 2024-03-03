package cz.jaro.dpmcb.ui.favourites.widget

import cz.jaro.dpmcb.data.helperclasses.LocalTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class KartickaWidgetState(
    val spojId: String,
    val linka: Int,
    val vychoziZastavka: String,
    @Serializable(with = LocalTimeSerializer::class) val vychoziZastavkaCas: LocalTime,
    val cilovaZastavka: String,
    @Serializable(with = LocalTimeSerializer::class) val cilovaZastavkaCas: LocalTime,
)