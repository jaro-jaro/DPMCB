package cz.jaro.dpmcb.ui.loading

interface AppUpdater {
    fun updateApp(
        loadingDialog: (String?) -> Unit,
    )
}