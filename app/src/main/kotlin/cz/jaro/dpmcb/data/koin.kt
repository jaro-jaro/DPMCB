package cz.jaro.dpmcb.data

import app.cash.sqldelight.db.SqlDriver
import cz.jaro.dpmcb.Database
import cz.jaro.dpmcb.data.database.createDatabase
import cz.jaro.dpmcb.ui.bus.BusViewModel
import cz.jaro.dpmcb.ui.card.CardViewModel
import cz.jaro.dpmcb.ui.chooser.ChooserViewModel
import cz.jaro.dpmcb.ui.departures.DeparturesViewModel
import cz.jaro.dpmcb.ui.favourites.FavouritesViewModel
import cz.jaro.dpmcb.ui.find_bus.FindBusViewModel
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.MainViewModel
import cz.jaro.dpmcb.ui.now_running.NowRunningViewModel
import cz.jaro.dpmcb.ui.sequence.SequenceViewModel
import cz.jaro.dpmcb.ui.settings.SettingsViewModel
import cz.jaro.dpmcb.ui.timetable.TimetableViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit

@KoinApplicationDslMarker
fun initKoin(platformSpecificModule: Module): KoinApplication {
    return startKoin {
        modules(platformSpecificModule, commonModule)
    }
}

val commonModule = module(true) {
    single {
        Retrofit.Builder()
            .baseUrl("https://mpvnet.cz/Jikord/")
            .build()
            .create(OnlineApi::class.java)
    }
    single {
        val driver = get<SqlDriver>()
        Database.Schema.create(driver)
        createDatabase(driver)
    }
    single {
        get<Database>().spojeQueries
    }
    single {
        SpojeRepository(get(), get(), get(), get(), get())
    }
    single {
        PreferenceDataSource(get())
    }
    single { OnlineRepository(get(), get(), get()) }
    viewModel { BusViewModel(get(), get(), it.get(), it.get()) }
    viewModel { CardViewModel(get()) }
    viewModel { ChooserViewModel(get(), it.get()) }
    viewModel { DeparturesViewModel(get(), get(), it.get()) }
    viewModel { FavouritesViewModel(get(), get(), it.get()) }
    viewModel { FindBusViewModel(get(), get(), it.get()) }
    viewModel { LoadingViewModel(get(), get(), get(), get(), it.get()) }
    viewModel { MainViewModel(get(), get(), get(), get(), it.get()) }
    viewModel { NowRunningViewModel(get(), get(), it.get()) }
    viewModel { SequenceViewModel(get(), get(), it.get()) }
    viewModel { SettingsViewModel(get(), get(), it.get()) }
    viewModel { TimetableViewModel(get(), it.get()) }
}