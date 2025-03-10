package cz.jaro.dpmcb

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.bundle.Bundle
import androidx.core.view.WindowCompat
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.ui.card.AndroidCardManager
import cz.jaro.dpmcb.ui.card.CardManager
import cz.jaro.dpmcb.ui.loading.AndroidAppUpdater
import cz.jaro.dpmcb.ui.loading.AppUpdater
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    val repo by inject<SpojeRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val updater = AndroidAppUpdater(this)
        val cardManager = AndroidCardManager(this, get())
        loadKoinModules(module(createdAtStart = true) {
            single<AppUpdater> { updater }
            single<CardManager> { cardManager }
        })

        val uri = intent?.action?.equals(Intent.ACTION_VIEW)?.let { intent?.data }?.run { toString().removePrefix("${scheme}://${host}") }

        setContent {
            SuperMainContent(repo, uri)
        }
    }
}