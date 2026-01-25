package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.observable.makeObservable
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.database.AndroidDriverFactory
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.AndroidDiagramManager
import cz.jaro.dpmcb.ui.map.DiagramManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.dsl.module

@OptIn(ExperimentalSettingsApi::class)
context(ctx: Context)
val androidModule get() = module(true) {
    single { ctx }
    single { Firebase.initialize(ctx)!! }
    single<SqlDriver> { AndroidDriverFactory(ctx).driver }
    single { ctx.getSharedPreferences("prefs-dpmcb", Context.MODE_PRIVATE) }
    single<ObservableSettings> { SharedPreferencesSettings(get()).makeObservable() }
    single { UserOnlineManager(ctx) }
    single { DetailsOpener(ctx) }
    single<DiagramManager> { AndroidDiagramManager(ctx) }
    single { DebugManager { BuildConfig.DEBUG } }
    single<Logger> { AndroidLogger(get()) }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val _ = initKoin(androidModule)
    }
}