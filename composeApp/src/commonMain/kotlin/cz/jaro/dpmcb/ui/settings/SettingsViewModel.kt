package cz.jaro.dpmcb.ui.settings

import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.BuildKonfig
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.SuperNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.combineStates
import cz.jaro.dpmcb.data.helperclasses.popUpTo
import cz.jaro.dpmcb.data.helperclasses.stateInViewModel
import cz.jaro.dpmcb.data.lineTraction
import cz.jaro.dpmcb.ui.loading.AppUpdater
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.SuperRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asFlow
import kotlin.time.Duration.Companion.seconds

class SettingsViewModel(
    private val repo: SpojeRepository,
    private val appUpdater: AppUpdater,
) : ViewModel() {

    lateinit var superNavigate: SuperNavigateFunction

    private val tables = suspend {
        repo.allTables.await().map {
            LineTable(
                shortNumber = it.shortNumber,
                tab = it.tab,
                validFrom = it.validFrom,
                validTo = it.validTo,
                route = it.route,
                traction = repo.lineTraction(it.number, it.vehicleType),
                hasRestriction = it.hasRestriction,
            )
        }.sortedWith(
            compareBy<LineTable> { it.shortNumber }.thenBy { it.validFrom }.thenBy { it.hasRestriction }
        )
    }
        .asFlow()
        .stateInViewModel(SharingStarted.WhileSubscribed(5.seconds), emptyList())

    val state = combineStates(
        repo.settings, repo.version, repo.isOnline, tables
    ) { settings, version, isOnline, tables ->
        SettingsState(
            settings = settings,
            version = BuildKonfig.versionName,
            dataVersion = version,
            dataMetaVersion = LoadingViewModel.META_DATA_VERSION,
            isOnline = isOnline,
            tables = tables,
        )
    }

    fun onEvent(e: SettingsEvent) = when (e) {
        is SettingsEvent.UpdateApp -> appUpdater.updateApp(e.loadingDialog)

        SettingsEvent.UpdateData -> {
            superNavigate(SuperRoute.Loading(null, true), popUpTo<SuperRoute.Main>())
        }

        is SettingsEvent.EditSettings -> repo.changeSettings(e.edit)
    }
}