package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.database.AppDatabase
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule

class App : Application() {

    companion object {
        var title by mutableStateOf(R.string.app_name)
        var vybrano by mutableStateOf(null as SuplikAkce?)
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
                            })
                        )
                    ) {
                        get<Context>().dataStoreFile("DPMCB_DataStore.preferences_pb")
                    }
                }
                single {
                    DopravaApi(
                        ctx = get(),
                        baseUrl = "https://www.dopravanajihu.cz/idspublicservices/api"
                    )
                }
                single {
                    Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "databaaaaze").fallbackToDestructiveMigration().build()
                }
                factory {
                    get<AppDatabase>().dao()
                }
            })
            defaultModule()
        }
    }
}
