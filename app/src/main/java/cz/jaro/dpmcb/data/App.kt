package cz.jaro.dpmcb.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.SuplikAkce
import cz.jaro.dpmcb.ui.jedouci.PraveJedouciViewModel
import cz.jaro.dpmcb.ui.oblibene.OblibeneViewModel
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyViewModel
import cz.jaro.dpmcb.ui.spoj.DetailSpojeViewModel
import cz.jaro.dpmcb.ui.vybirator.VybiratorViewModel
import cz.jaro.dpmcb.ui.zjr.JizdniRadyViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {

    companion object {

        var title by mutableStateOf(R.string.app_name)
        var vybrano by mutableStateOf(null as SuplikAkce?)

        lateinit var repo: SpojeRepository
        lateinit var dopravaRepo: DopravaRepository
    }

    override fun onCreate() {
        super.onCreate()
        repo = SpojeRepository(this)
        dopravaRepo = DopravaRepository(this)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(module {

                viewModel {
                    OdjezdyViewModel(it.component1(), it.component2())
                }
                viewModel {
                    JizdniRadyViewModel(it.component1(), it.component2(), it.component3())
                }
                viewModel {
                    VybiratorViewModel(it.component1(), it.component2(), it.component3(), it.component4(), it.component5())
                }
                viewModel {
                    DetailSpojeViewModel(it.component1())
                }
                viewModel {
                    PraveJedouciViewModel()
                }
                viewModel {
                    OblibeneViewModel()
                }
            })
        }
    }
}
