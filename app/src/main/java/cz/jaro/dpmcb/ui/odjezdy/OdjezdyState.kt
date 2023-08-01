package cz.jaro.dpmcb.ui.odjezdy

sealed interface OdjezdyState {
    object Loading : OdjezdyState

    sealed interface NicNejede : OdjezdyState

    object VubecNicNejede : NicNejede
    object SemNicNejede : NicNejede
    object LinkaNejede : NicNejede
    object LinkaSemNejede : NicNejede

    data class Jede(
        val seznam: List<KartickaState>,
    ) : OdjezdyState
}