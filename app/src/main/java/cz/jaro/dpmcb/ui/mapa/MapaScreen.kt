package cz.jaro.dpmcb.ui.mapa

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.annotation.Destination
import cz.jaro.dpmcb.databinding.FragmentMapaBinding
import java.io.InputStream

@Destination
@Composable
fun MapaScreen(

) {

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->

            val binding = FragmentMapaBinding.inflate(LayoutInflater.from(context))

            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("Schema 2022_01.pdf")

            binding.pdfMapa.fromStream(inputStream).apply {

                load()
            }

            binding.root
        }
    )
}
