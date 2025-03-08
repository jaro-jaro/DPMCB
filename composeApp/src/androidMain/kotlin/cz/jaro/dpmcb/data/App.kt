package cz.jaro.dpmcb.data

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import cz.jaro.dpmcb.Database
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.AndroidDiagramManager
import cz.jaro.dpmcb.ui.map.DiagramManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.dsl.bind

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val ctx = this
        initKoin(true) {
            single { this@App } bind Context::class
            single { Firebase.initialize(get<Context>())!! }
            single<SqlDriver> { AndroidSqliteDriver(Database.Schema, ctx, "test.db") }
            single { get<Context>().getSharedPreferences("prefs-dpmcb", MODE_PRIVATE) }
            single { SharedPreferencesSettings(get()) } bind ObservableSettings::class
            single { UserOnlineManager(ctx) }
            single { DetailsOpener(ctx) }
            single<DiagramManager> { AndroidDiagramManager(ctx) }
        }
    }
}