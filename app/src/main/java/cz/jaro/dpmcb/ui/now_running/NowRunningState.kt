package cz.jaro.dpmcb.ui.now_running

sealed interface NowRunningState {

    data object IsNotToday : NowRunningState

    data object Offline : NowRunningState

    data class LoadingLines(
        override val type: NowRunningType,
    ) : HasType

    data object NoLines : NowRunningState

    sealed interface HasFilters : HasType {
        val lineNumbers: List<Int>
        val filters: List<Int>
    }

    sealed interface HasType : NowRunningState {
        val type: NowRunningType
    }

    sealed interface HasNotRunning : NowRunningState {
        val nowNotRunning: List<Pair<String, String>>
    }

//    data class NeniNicVybrano(
//        override val cislaLinek: List<Int>,
//    ) : LinkyNacteny {
//        override val filtry: List<Int> = emptyList()
//    }

    data class Loading(
        override val lineNumbers: List<Int>,
        override val filters: List<Int>,
        override val type: NowRunningType,
    ) : HasFilters, HasType

    data class NothingRunningNow(
        override val lineNumbers: List<Int>,
        override val filters: List<Int>,
        override val type: NowRunningType,
        override val nowNotRunning: List<Pair<String, String>>,
    ) : HasFilters, HasType, HasNotRunning

    data class OK(
        override val lineNumbers: List<Int>,
        override val filters: List<Int>,
        override val type: NowRunningType,
        override val nowNotRunning: List<Pair<String, String>>,
        val result: NowRunningResults<*>,
    ) : HasFilters, HasType, HasNotRunning
}