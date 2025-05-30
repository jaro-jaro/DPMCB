package cz.jaro.dpmcb

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.StorageSettings
import com.russhwolf.settings.observable.makeObservable
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.UserOnlineManager
import cz.jaro.dpmcb.data.database.SpojeDataSource
import cz.jaro.dpmcb.data.database.SupabaseDataSource
import cz.jaro.dpmcb.data.initKoin
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.loading.AppUpdater
import cz.jaro.dpmcb.ui.main.DetailsOpener
import cz.jaro.dpmcb.ui.map.DiagramManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.storage.StorageReference
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.skiko.wasm.onWasmReady
import org.koin.compose.LocalKoinApplication
import org.koin.compose.LocalKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.module

@OptIn(ExperimentalComposeUiApi::class, ExperimentalSettingsApi::class, KoinInternalApi::class)
fun main() = try {
    val webModule = module(false) {
        single<FirebaseApp> {
            Firebase.initialize(
                null, FirebaseOptions(
                    apiKey = "AIzaSyA9O1-nYFEmY0pszqGV5AyXPvJLIsuwvFg",
                    authDomain = "dpmcb-jaro.firebaseapp.com",
                    databaseUrl = "https://dpmcb-jaro-default-rtdb.europe-west1.firebasedatabase.app/",
                    projectId = "dpmcb-jaro",
                    storageBucket = "dpmcb-jaro.appspot.com",
                    gcmSenderId = "867578529394",
                    applicationId = "1:867578529394:web:651bf2325825b415bba4eb",
                    gaTrackingId = "G-EBQL901DWT",
                )
            )
        }
        single {
            createSupabaseClient(
                supabaseUrl = "https://ygbqqztfvcnqxxbqvxwb.supabase.co",
                supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlnYnFxenRmdmNucXh4YnF2eHdiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDE1ODgyNDksImV4cCI6MjA1NzE2NDI0OX0.6e2CrFnDrBAV-GN_rwt8l9TbC-qfQaiMdbYemUcRYUY"
            ) {
                install(Postgrest)
                install(Auth)
            }.also {
                CoroutineScope(Dispatchers.Default).launch {
                    it.auth.signInAnonymously()
                }
            }
        }
        single<SpojeDataSource> { SupabaseDataSource(get()) }
        single<UserOnlineManager> { UserOnlineManager { true } }
        single { StorageSettings().makeObservable() }
        single<DetailsOpener> { DetailsOpener {} }
        single<DiagramManager> {
            object : DiagramManager {
                override suspend fun downloadDiagram(reference: StorageReference, progress: (Float) -> Unit) = Unit
                override val imageData = null
                override fun checkDiagram() = true
            }
        }
        single<AppUpdater> { AppUpdater {} }
        single<CardManager> {
            object : CardManager {
                override fun loadCard() = Unit
                override fun removeCard() = Unit
                override val card = MutableStateFlow(null)
            }
        }
    }
    val koinApp = initKoin(webModule)

    val repo = koinApp.koin.get<SpojeRepository>()

    val link = window.location.hash.removePrefix("#") + window.location.search
    onWasmReady {
        CanvasBasedWindow(
            title = "Lepší DPMCB",
        ) {
            CompositionLocalProvider(
                LocalKoinApplication provides koinApp.koin,
                LocalKoinScope provides koinApp.koin.scopeRegistry.rootScope
            ) {
                SuperMainContent(repo, link.takeUnless { it.isBlank() })
            }
        }
    }
} catch (e: Exception) {
    e.printStackTrace()
}