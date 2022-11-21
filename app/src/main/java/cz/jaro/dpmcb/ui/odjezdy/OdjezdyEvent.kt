package cz.jaro.dpmcb.ui.odjezdy

sealed class OdjezdyEvent {
    object ZmensitCas: OdjezdyEvent()
    object ZvetsitCas: OdjezdyEvent()
    data class KliklNaDetailSpoje(val spoj: Long): OdjezdyEvent()
    data class KliklNaZjr(val spoj: OdjezdyViewModel.KartickaState): OdjezdyEvent()
    object NacistDalsi: OdjezdyEvent()
    object NacistPredchozi: OdjezdyEvent()
}
