package cz.jaro.dpmcb.ui.detail.kurzu

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.databinding.FragmentDetailKurzuBinding
import kotlinx.coroutines.runBlocking

@Destination
@Composable
fun DetailKurzuScreen(
    navigator: DestinationsNavigator,
    kurz: String
) {

    App.title = R.string.detail_spoje

    val spoje = runBlocking { repo.spojeKurzu(kurz) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val binding = FragmentDetailKurzuBinding.inflate(LayoutInflater.from(context))

            binding.tvInfo.text = context.getString(R.string.kurz_tento, kurz)

            binding.rvKurz.layoutManager = LinearLayoutManager(context)
            binding.rvKurz.adapter = KurzAdapter(navigator, context, spoje)

            binding.root
        },
    )
}
