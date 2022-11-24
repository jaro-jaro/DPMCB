package cz.jaro.dpmcb.ui.mapa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.ramcosta.composedestinations.annotation.Destination
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App.Companion.title
import java.io.InputStream

@Destination
@Composable
fun MapaScreen(

) {
    title = R.string.mapa_linek

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PDFView(context, null).apply {

                val assetManager = context.assets
                val inputStream: InputStream = assetManager.open("Schema 2022_01.pdf")

                fromStream(inputStream).load()
            }
        }
    )
}
