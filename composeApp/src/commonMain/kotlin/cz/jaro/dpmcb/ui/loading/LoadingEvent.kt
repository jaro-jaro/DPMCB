package cz.jaro.dpmcb.ui.loading

sealed interface LoadingEvent {
    data object DownloadDataIfError : LoadingEvent
}