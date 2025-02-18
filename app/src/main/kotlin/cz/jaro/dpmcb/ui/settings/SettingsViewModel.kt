package cz.jaro.dpmcb.ui.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.SuperRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SettingsViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val startActivity: (Intent) -> Unit,
        val youAreOfflineToast: () -> Unit,
    )

    lateinit var superNavigate: SuperNavigateFunction

    val state = combine(repo.settings, repo.version, repo.isOnline) { settings, version, isOnline ->
        SettingsState(
            settings = settings,
            version = BuildConfig.VERSION_NAME,
            dataVersion = version,
            dataMetaVersion = LoadingViewModel.META_DATA_VERSION,
            isOnline = isOnline,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SettingsState(Settings(), "", 0, 0, false))

    fun onEvent(e: SettingsEvent) = when (e) {
        SettingsEvent.UpdateApp -> {
            params.startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = "https://github.com/jaro-jaro/DPMCB/releases/latest".toUri()
            })
        }

        SettingsEvent.UpdateData -> {
            if (repo.isOnline.value) {
                superNavigate(SuperRoute.Loading(null, true), popUpTo<SuperRoute.Main>())
            } else
                params.youAreOfflineToast()
        }

        is SettingsEvent.EditSettings -> {
            viewModelScope.launch {
                repo.editSettings(e.edit)
            }
            Unit
        }
    }
}