package cz.jaro.dpmcb.ui.oblibene

import java.time.LocalDate

sealed interface OblibeneState {

    data object NacitaSe : OblibeneState

    data object ZadneOblibene : OblibeneState

    sealed interface NekdyNecoJede : OblibeneState {
        val dnes: LocalDate
    }

    sealed interface DnesNecoJede : NekdyNecoJede {
        val dnesJede: List<KartickaState>
    }

    sealed interface JindyNecoJede : NekdyNecoJede {
        val jindyJede: List<KartickaState.Offline>
    }

    data class JedeJenDnes(
        override val dnesJede: List<KartickaState>,
        override val dnes: LocalDate,
    ) : DnesNecoJede

    data class JedeJenJindy(
        override val jindyJede: List<KartickaState.Offline>,
        override val dnes: LocalDate,
    ) : JindyNecoJede

    data class JedeFurt(
        override val dnesJede: List<KartickaState>,
        override val jindyJede: List<KartickaState.Offline>,
        override val dnes: LocalDate,
    ) : DnesNecoJede, JindyNecoJede
}
