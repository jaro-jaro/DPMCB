package cz.jaro.dpmcb.ui.odjezdy

sealed interface OdjezdyState {
    data object Loading : OdjezdyState

    sealed interface NicNejede : OdjezdyState

    data object VubecNicNejede : NicNejede
    data object SemNicNejede : NicNejede
    data object LinkaNejede : NicNejede
    data object LinkaSemNejede : NicNejede

    data class Jede(
        val seznam: List<KartickaState>,
    ) : OdjezdyState
}