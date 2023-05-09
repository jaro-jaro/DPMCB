package cz.jaro.dpmcb.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.ui.jedouci.PraveJedouciViewModel
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
import cz.jaro.dpmcb.ui.main.MainViewModel
import cz.jaro.dpmcb.ui.main.SuplikAkce
import cz.jaro.dpmcb.ui.nastaveni.NastaveniViewModel
import cz.jaro.dpmcb.ui.oblibene.OblibeneViewModel
import cz.jaro.dpmcb.ui.odjezdy.OdjezdyViewModel
import cz.jaro.dpmcb.ui.spoj.SpojViewModel
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
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(module {

                single {
                    SpojeRepository(this@App)
                }

                single {
                    DopravaRepository(get(), this@App)
                }

                viewModel {
                    OdjezdyViewModel(get(), get(), it[0], it[1], it[2], it[3])
                }
                viewModel {
                    JizdniRadyViewModel(get(), it[0], it[1], it[2])
                }
                viewModel {
                    VybiratorViewModel(get(), it[0], it[1], it[2], it[3], it[4])
                }
                viewModel {
                    SpojViewModel(get(), get(), it[0])
                }
                viewModel {
                    PraveJedouciViewModel(get(), get(), it[0])
                }
                viewModel {
                    OblibeneViewModel(get(), get())
                }
                viewModel {
                    LoadingViewModel(get(), it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], it[9], it[10], it[11])
                }
                viewModel {
                    MainViewModel(get(), it[0], it[1], it[2], it[3], it[4])
                }
                viewModel {
                    NastaveniViewModel(get(), it[0], it[1], it[2], it[3])
                }
            })
        }
    }
}
