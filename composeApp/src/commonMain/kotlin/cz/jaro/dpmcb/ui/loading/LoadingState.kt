package cz.jaro.dpmcb.ui.loading

sealed interface LoadingState {
    data object Offline : LoadingState
    data object Error : LoadingState
    data class Loading(
        val infoText: String = "",
        val progress: Float? = null,
    ) : LoadingState
}