package cz.jaro.dpmcb.data

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.jaro.dpmcb.data.App.Companion.isOnline
import org.koin.dsl.module

val platformModule = module(true) {
    single {
        PreferenceDataStoreFactory.create(
            migrations = listOf()
        ) {
            get<Context>().dataStoreFile("DPMCB_DataStore.preferences_pb")
        }
    }
    single { UserOnlineManager { get<Context>().isOnline() } }
}