package cz.jaro.dpmcb.ui.kurz

sealed interface KurzState {

    data object Loading : KurzState

    data class Neexistuje(
        val kurzId: String,
    ) : KurzState

    data class OK(
        val kurzId: String,
        val spoje: List<SpojKurzuState>,
    ) : KurzState
}