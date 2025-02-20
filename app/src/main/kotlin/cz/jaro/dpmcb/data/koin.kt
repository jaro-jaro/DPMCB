package cz.jaro.dpmcb.data

import android.content.Context
import androidx.room.Room
import cz.jaro.dpmcb.data.database.AppDatabase
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
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val commonModule = module(true) {
    single {
        Retrofit.Builder()
            .baseUrl("https://mpvnet.cz/Jikord/")
            .build()
            .create(OnlineApi::class.java)
    }
    single {
        Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "databaaaaze")
            .fallbackToDestructiveMigration()
            .build()
    }
    factory {
        get<AppDatabase>().dao()
    }
    single {
        SpojeRepository(get(), get(), get(), get())
    }
    single {
        PreferenceDataSource(get())
    }
    single { OnlineRepository(get(), get(), get()) } bind UserOnlineManager::class
    viewModel { BusViewModel(get(), get(), it.get(), it.get()) }
    viewModel { CardViewModel(get(), it.get()) }
    viewModel { ChooserViewModel(get(), it.get()) }
    viewModel { DeparturesViewModel(get(), get(), it.get()) }
    viewModel { FavouritesViewModel(get(), get(), it.get()) }
    viewModel { FindBusViewModel(get(), get(), it.get()) }
    viewModel { LoadingViewModel(get(), get(), it.get()) }
    viewModel { MainViewModel(get(), get(), get(), it.get()) }
    viewModel { NowRunningViewModel(get(), get(), it.get()) }
    viewModel { SequenceViewModel(get(), get(), it.get()) }
    viewModel { SettingsViewModel(get(), get(), it.get()) }
    viewModel { TimetableViewModel(get(), it.get()) }
}

