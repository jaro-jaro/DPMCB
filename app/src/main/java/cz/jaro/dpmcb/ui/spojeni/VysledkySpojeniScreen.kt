package cz.jaro.dpmcb.ui.spojeni

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.databinding.FragmentVysledkySpojeniBinding
import cz.jaro.dpmcb.ui.destinations.DetailSpojeScreenDestination
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.get

@Destination
@Composable
fun VysledkySpojeniScreen(
    navigator: DestinationsNavigator,
) {
    App.title = R.string.detail_spoje

    val repository: SpojeRepository = get()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val binding = FragmentVysledkySpojeniBinding.inflate(LayoutInflater.from(context))

            binding.btn2.setOnClickListener {
                runBlocking {
                    navigator.navigate(
                        DetailSpojeScreenDestination(
                            repository.spoje().first().id
                        )
                    )
                }
            }
            binding.root
        },
    )
}
