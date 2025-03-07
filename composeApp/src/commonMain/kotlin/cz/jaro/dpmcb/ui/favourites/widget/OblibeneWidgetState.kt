package cz.jaro.dpmcb.ui.favourites.widget

import kotlinx.serialization.Serializable

@Serializable
sealed interface OblibeneWidgetState {
    @Serializable
    data object Error : OblibeneWidgetState

    @Serializable
    data object NacitaSe : OblibeneWidgetState

    @Serializable
    data object ZadneOblibene : OblibeneWidgetState

    @Serializable
    data object PraveNicNejede : OblibeneWidgetState

    @Serializable
    data class TedJede(
        val spoje: List<KartickaWidgetState>,
    ) : OblibeneWidgetState
}