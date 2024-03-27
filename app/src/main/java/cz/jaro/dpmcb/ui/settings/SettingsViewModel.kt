package cz.jaro.dpmcb.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
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
        val loadingActivityIntent: Intent,
        val youAreOfflineToast: () -> Unit,
        val navigateBack: () -> Unit,
    )

    val state = repo.settings.combine(repo.version) { settings, version ->
        SettingsState(
            settings = settings,
            version = BuildConfig.VERSION_NAME,
            dataVersion = version,
            dataMetaVersion = LoadingViewModel.META_DATA_VERSION,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), SettingsState(Settings(), "", 0, 0))

    fun onEvent(e: SettingsEvent) = when (e) {
        SettingsEvent.UpdateApp -> {
            params.startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/latest")
            })
        }

        SettingsEvent.UpdateData -> {
            if (repo.isOnline.value) {
                params.startActivity(params.loadingActivityIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                    putExtra("update", true)
                })
                params.navigateBack()
            } else
                params.youAreOfflineToast()
        }

        SettingsEvent.NavigateBack -> {
            params.navigateBack()
        }

        is SettingsEvent.EditSettings -> {
            viewModelScope.launch {
                repo.editSettings(e.edit)
            }
            Unit
        }
    }
}