package cz.jaro.dpmcb.ui.loading

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.ExitActivity
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.MainActivity
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.schemaFile
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import kotlin.system.exitProcess

@Composable
fun Loading(
    uri: String?,
    update: Boolean,
    finish: () -> Unit,
) {
    val ctx = LocalContext.current

    var callback by remember { mutableStateOf({}) }
    var chybaDialog by remember { mutableStateOf(false) }

    val viewModel: LoadingViewModel = koinViewModel {
        parametersOf(
            LoadingViewModel.Parameters(
                uri = uri,
                update = update,
                chyba = { it: () -> Unit ->
                    callback = it
                    chybaDialog = true
                },
                potrebaInternet = {
                    Toast.makeText(ctx, "Na stažení jizdních řádů je potřeba připojení k internetu!", Toast.LENGTH_LONG).show()
                },
                finish = finish,
                schemaFile = ctx.schemaFile,
                jrFile = File(ctx.cacheDir, "jr-dpmcb.jaro"),
                mainActivityIntent = Intent(ctx, MainActivity::class.java),
                loadingActivityIntent = Intent(ctx, LoadingActivity::class.java),
                startActivity = { it: Intent ->
                    ctx.startActivity(it)
                },
                packageName = ctx.packageName,
                exit = {
                    ExitActivity.exitApplication(ctx)
                    exitProcess(0)
                }
            )
        )
    }

    if (chybaDialog) AlertDialog(
        onDismissRequest = {
            ExitActivity.exitApplication(ctx)
            exitProcess(0)
        },
        confirmButton = {
            TextButton(onClick = {
                chybaDialog = false
                callback()
            }) {
                Text(text = "Ano")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                ExitActivity.exitApplication(ctx)
                exitProcess(0)
            }) {
                Text(text = "Ne, zavřít aplikaci")
            }
        },
        title = {
            Text(text = "Chyba!")
        },
        text = {
            Text(text = "Zdá se, ža vaše jizdní řády uložené v zařízení jsou poškozené! Chcete stáhnout nové?")
        }
    )

    val state by viewModel.state.collectAsStateWithLifecycle()

    val nastaveni by viewModel.nastaveni.collectAsStateWithLifecycle()

    LoadingScreen(
        progress = state.second,
        infoText = state.first,
        darkMode = if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm,
    )
}

@Composable
fun LoadingScreen(
    progress: Float?,
    infoText: String,
    darkMode: Boolean,
) {

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Icons.Default.Print
        Column(
            Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(
                    if (darkMode) R.drawable.logo_jaro_black else R.drawable.logo_jaro_white
                ),
                contentDescription = "Logo JARO",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
                colorFilter = ColorFilter.colorMatrix(ColorMatrix())
            )
            Text(infoText, textAlign = TextAlign.Center)
            if (progress == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            } else {
                val animatedProgress by animateFloatAsState(progress, label = "Loading progress", animationSpec = spring(dampingRatio = 2F))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }

}