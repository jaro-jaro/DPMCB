package cz.jaro.dpmcb.data

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.AndroidDiagramManager
import cz.jaro.dpmcb.ui.map.DiagramManager
import org.koin.dsl.module

val platformModule = module(true) {
    single {
        PreferenceDataStoreFactory.create(
            migrations = listOf()
        ) {
            get<Context>().dataStoreFile("DPMCB_DataStore.preferences_pb")
        }
    }
    single { UserOnlineManager(get()) }
    single { DetailsOpener(get()) }
    single<DiagramManager> { AndroidDiagramManager(get()) }
}
