package cz.jaro.dpmcb.ui.detail.spoje

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.updateLayoutParams
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.zastavkySpoje
import cz.jaro.dpmcb.databinding.FragmentDetailSpojeBinding
import cz.jaro.dpmcb.ui.destinations.DetailKurzuScreenDestination
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import kotlinx.coroutines.runBlocking

@Destination
@Composable
fun DetailSpojeScreen(
    navigator: DestinationsNavigator,
    spojId: Long,
) {

    App.title = R.string.detail_spoje

    val spoj = runBlocking { repo.spoj(spojId) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val binding = FragmentDetailSpojeBinding.inflate(LayoutInflater.from(context))

            binding.tvInfo.text = context.getString(R.string.linka_tato, spoj.cisloLinky.toString())

            binding.btn3.setOnClickListener {

                navigator.navigate(
                    DetailKurzuScreenDestination(
                        kurz = spoj.nazevKurzu
                    )
                )

            }
            val odjezdy = { z: String, cas: String ->

                navigator.navigate(
                    OdjezdyScreenDestination(
                        cas = cas,
                        zastavka = z,
                    )
                )
            }

            when (spoj.smer) {
                Smer.POZITIVNI -> runBlocking { spoj.zastavkySpoje() }
                Smer.NEGATIVNI -> runBlocking { spoj.zastavkySpoje().reversed() }
            }.filter { it.cas != Cas.nikdy }.forEach { zastavkaSCasem ->
                val z = zastavkaSCasem.nazevZastavky
                val cas = zastavkaSCasem.cas

                binding.included.llZastavky.addView(TextView(context).apply {
                    text = z
                    setOnClickListener { odjezdy(z, cas.toString()) }
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    updateLayoutParams<LinearLayout.LayoutParams> {
                        minHeight = 48
                        minWidth = 48
                    }
                })

                binding.included.llCasy.addView(TextView(context).apply {
                    text = cas.toString()
                    setOnClickListener { odjezdy(z, cas.toString()) }
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    updateLayoutParams<LinearLayout.LayoutParams> {
                        minHeight = 48
                        minWidth = 48
                    }
                })
            }

            binding.root
        },
    )
}
