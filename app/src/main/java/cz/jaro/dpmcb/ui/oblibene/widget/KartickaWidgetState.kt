package cz.jaro.dpmcb.ui.oblibene.widget

import cz.jaro.dpmcb.data.helperclasses.LocalTimeSeralizer
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class KartickaWidgetState(
    val spojId: String,
    val linka: Int,
    val vychoziZastavka: String,
    @Serializable(with = LocalTimeSeralizer::class) val vychoziZastavkaCas: LocalTime,
    val cilovaZastavka: String,
    @Serializable(with = LocalTimeSeralizer::class) val cilovaZastavkaCas: LocalTime,
)