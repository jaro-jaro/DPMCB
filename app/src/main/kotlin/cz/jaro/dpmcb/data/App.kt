package cz.jaro.dpmcb.data

import android.app.Application
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import cz.jaro.dpmcb.Database
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.AndroidDiagramManager
import cz.jaro.dpmcb.ui.map.DiagramManager
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

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

        val ctx = this
        startKoin {
            androidLogger()
            modules(commonModule)
            modules(platformModule(ctx))
        }
    }
}

private fun platformModule(ctx: App) = module(true) {
    single<SqlDriver> { AndroidSqliteDriver(Database.Schema, ctx, "test.db") }
    single {
        PreferenceDataStoreFactory.create(
            migrations = listOf()
        ) {
            ctx.dataStoreFile("DPMCB_DataStore.preferences_pb")
        }
    }
    single { UserOnlineManager(ctx) }
    single { DetailsOpener(ctx) }
    single<DiagramManager> { AndroidDiagramManager(ctx) }
}
