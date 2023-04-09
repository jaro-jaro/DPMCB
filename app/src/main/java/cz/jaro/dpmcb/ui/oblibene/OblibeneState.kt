package cz.jaro.dpmcb.ui.oblibene

data class OblibeneState(
    val nacitaSe: Boolean,
    val nejake: Boolean,
    val dnes: List<KartickaState>,
    val jindy: List<KartickaState>,
)