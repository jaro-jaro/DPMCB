package cz.jaro.dpmcb.ui.prukazka

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.ui.main.SuplikAkce
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

var callback = { _: Uri? -> }

@Composable
@Destination
fun Prukazka() {
    App.title = R.string.nic
    App.vybrano = SuplikAkce.Prukazka


    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        callback(it)
    }

    fun getPickMultipleMedia(cb: (Uri?) -> Unit): ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> {
        callback = cb
        return launcher
    }

    val viewModel: PrukazkaViewModel = koinViewModel {
        parametersOf(PrukazkaViewModel.Parameters(
            getPickMultipleMedia = ::getPickMultipleMedia
        ))
    }

    val maPrukazku by viewModel.maPrukazku.collectAsStateWithLifecycle()
    val prukazka by viewModel.prukazka.collectAsStateWithLifecycle()

    if (maPrukazku != null) PrukazkaScreen(
        prukazka = prukazka,
        maPrukazku = maPrukazku!!,
        pridatPrukazku = viewModel::pridatPrukazku,
    )
}

@Composable
fun PrukazkaScreen(
    prukazka: ImageBitmap?,
    maPrukazku: Boolean,
    pridatPrukazku: () -> Unit,
) {
    Box(
        Modifier.background(Color(0xFFD73139)).fillMaxSize()
    ) {
        if (!maPrukazku || prukazka == null) {
            Button(
                onClick = {
                    pridatPrukazku()
                },
                Modifier.padding(16.dp)
            ) {
                Text("Přidat průkazku")
            }
        } else {
            Image(
                bitmap = prukazka,
                contentDescription = null,
                Modifier.fillMaxWidth().padding(all = 8.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
