package cz.jaro.dpmcb.ui.oblibene.widget

import kotlinx.serialization.Serializable

@Serializable
sealed interface OblibeneWidgetState {
    @Serializable
    object Error : OblibeneWidgetState

    @Serializable
    object NacitaSe : OblibeneWidgetState

    @Serializable
    object ZadneOblibene : OblibeneWidgetState

    @Serializable
    object PraveNicNejede : OblibeneWidgetState

    @Serializable
    data class TedJede(
        val spoje: List<KartickaWidgetState>,
    ) : OblibeneWidgetState
}