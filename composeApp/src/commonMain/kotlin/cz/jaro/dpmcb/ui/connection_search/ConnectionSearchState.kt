package cz.jaro.dpmcb.ui.connection_search

data class ConnectionSearchState(
    val settings: SearchSettings,
    val settingsModified: Boolean,
    val history: List<SearchSettings>,
)