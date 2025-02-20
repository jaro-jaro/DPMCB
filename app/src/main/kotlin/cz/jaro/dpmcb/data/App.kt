package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule
import retrofit2.Retrofit

class App : Application() {

    init {
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl"
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl"
        )
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.stax.EventFactoryImpl"
        )
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(module(true) {
                single {
                    PreferenceDataStoreFactory.create(
                        migrations = listOf(
                            SharedPreferencesMigration({
                                get<Context>().getSharedPreferences("PREFS_DPMCB_JARO", Context.MODE_PRIVATE)
                            }),
                            DataStoreMigrationConnName(),
                        )
                    ) {
                        get<Context>().dataStoreFile("DPMCB_DataStore.preferences_pb")
                    }
                }
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
                viewModel { params ->
                    LoadingViewModel(get(), get(), params.get())
                }
                viewModel { params ->
                    SettingsViewModel(get(), get(), params.get())
                }
                single { UserOnlineManager { get<Context>().isOnline() } }
            })
            defaultModule()
        }
    }

    companion object {
        fun Context.isOnline(): Boolean {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false

            return capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            )
        }
    }
}
