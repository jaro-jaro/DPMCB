package cz.jaro.dpmcb.ui.odjezdy

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

fun List<KartickaState>.domov(info: OdjezdyInfo) = (withIndex().firstOrNull { (_, zast) ->
    zast.cas + (if (zast.jedeZa > Duration.ZERO && zast.zpozdeni != null) zast.zpozdeni.toDouble().minutes else 0.seconds) >= info.cas
}?.index ?: lastIndex) + 1