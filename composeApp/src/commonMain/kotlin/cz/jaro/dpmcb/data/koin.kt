package cz.jaro.dpmcb.data

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import cz.jaro.dpmcb.ui.bus.BusViewModel
import cz.jaro.dpmcb.ui.card.CardViewModel
import cz.jaro.dpmcb.ui.chooser.ChooserViewModel
import cz.jaro.dpmcb.ui.connection.ConnectionViewModel
import cz.jaro.dpmcb.ui.connection_results.ConnectionResultsViewModel
import cz.jaro.dpmcb.ui.connection_search.ConnectionSearchViewModel
import cz.jaro.dpmcb.ui.departures.DeparturesViewModel
//import cz.jaro.dpmcb.ui.favourites.FavouritesViewModel
import cz.jaro.dpmcb.ui.find_bus.FindBusViewModel
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.MainViewModel
import cz.jaro.dpmcb.ui.now_running.NowRunningViewModel
import cz.jaro.dpmcb.ui.sequence.SequenceViewModel
import cz.jaro.dpmcb.ui.settings.SettingsViewModel
import cz.jaro.dpmcb.ui.timetable.TimetableViewModel
import org.koin.compose.LocalKoinApplication
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.startKoin
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

@KoinApplicationDslMarker
fun initKoin(
    platformSpecificModule: Module,
): KoinApplication = startKoin {
    modules(platformSpecificModule, commonModule)
}

val commonModule = module(true) {
    single<SpojeRepository> { SpojeRepository(get(), get(), get(), get()) }
    single<GlobalSettingsDataSource> { RemoteConfigDataSource(get(), get()) }
    single<LocalSettingsDataSource> { MultiplatformSettingsDataSource(get()) }
    single { OnlineModeManager(get(), get()) }
    single { OnlineRepository(get(), get()) }
    factory { BusViewModel(get(), get(), get(), it.get()) }
    factory { CardViewModel(get()) }
    factory { ChooserViewModel(get(), it.get()) }
    factory { DeparturesViewModel(get(), get(), get(), it.get()) }
//    factory { FavouritesViewModel(get(), get()) }
    factory { FindBusViewModel(get(), get(), it.get()) }
    factory { LoadingViewModel(get(), get(), get(), it.get()) }
    factory { MainViewModel(get(), get(), get(), get(), get(), it.get()) }
    factory { NowRunningViewModel(get(), get(), get(), it.get()) }
    factory { SequenceViewModel(get(), get(), it.get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { TimetableViewModel(get(), it.get()) }
    factory { ConnectionSearchViewModel(get(), get(), it.get()) }
    factory { ConnectionResultsViewModel(get(), it.get()) }
    factory { ConnectionViewModel(get(), it.get()) }
}

@OptIn(KoinInternalApi::class)
@Composable
inline fun <reified VM : ViewModel> viewModel(vararg params: Any? = arrayOf()): VM {
    val koin = LocalKoinApplication.current.getValue()
    return androidx.lifecycle.viewmodel.compose.viewModel<VM>(initializer = { koin.get<VM> { parametersOf(*params) } })
}