package cz.jaro.dpmcb.ui.nastaveni

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.Nastaveni
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
class NastaveniViewModel(
    private val repo: SpojeRepository,
    @InjectedParam private val params: Parameters,
) : ViewModel() {

    data class Parameters(
        val startActivity: (Intent) -> Unit,
        val loadingActivityIntent: Intent,
        val jsteOfflineToast: () -> Unit,
        val navigateBack: () -> Unit,
    )

    val state = repo.nastaveni.combine(repo.verze) { nastaveni, verze ->
        NastaveniState(
            nastaveni = nastaveni,
            verze = BuildConfig.VERSION_NAME,
            verzeDat = verze,
            metaVerzeDat = LoadingViewModel.META_VERZE_DAT,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), NastaveniState(Nastaveni(), "", 0, 0))

    fun onEvent(e: NastaveniEvent) = when (e) {
        NastaveniEvent.AktualizovatAplikaci -> {
            params.startActivity(Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://github.com/jaro-jaro/DPMCB/releases/latest")
            })
        }

        NastaveniEvent.AktualizovatData -> {
            if (repo.isOnline.value) {
                params.startActivity(params.loadingActivityIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                    putExtra("update", true)
                })
                params.navigateBack()
            } else
                params.jsteOfflineToast()
        }

        NastaveniEvent.NavigateBack -> {
            params.navigateBack()
        }

        is NastaveniEvent.UpravitNastaveni -> {
            viewModelScope.launch {
                repo.upravitNastaveni(e.upravit)
            }
            Unit
        }
    }
}