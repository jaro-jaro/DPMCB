package cz.jaro.dpmcb.ui.loading

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.helperclasses.diagramFile
import cz.jaro.dpmcb.data.helperclasses.nowFlow
import cz.jaro.dpmcb.data.helperclasses.superNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.two
import cz.jaro.dpmcb.ui.main.SuperRoute
import cz.jaro.dpmcb.ui.theme.LocalIsDarkThemeUsed
import kotlinx.coroutines.Job
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@Composable
fun Loading(
    navController: NavHostController,
    args: SuperRoute.Loading,
    viewModel: LoadingViewModel = run {
        val ctx = LocalContext.current
        koinViewModel {
            parametersOf(
                LoadingViewModel.Parameters(
                    update = args.update == true,
                    diagramFile = ctx.diagramFile,
                    dataFile = File(ctx.cacheDir, "jr-dpmcb.jaro"),
                    sequencesFile = File(ctx.cacheDir, "kurzy.jaro"),
                    link = args.link,
                )
            )
        }
    },
) {
    LaunchedEffect(Unit) {
        viewModel.navigate = navController.superNavigateFunction
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LoadingScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun LoadingScreen(
    state: LoadingState,
    onEvent: (LoadingEvent) -> Job,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Box(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val time by nowFlow.collectAsStateWithLifecycle()
                Text(
                    text = "${time.hour.two()}:${time.minute.two()}:${time.second.two()}",
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .38F),
                )
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(
                    if (LocalIsDarkThemeUsed.current) R.drawable.logo_jaro_black else R.drawable.logo_jaro_white
                ),
                contentDescription = "Logo JARO",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
                colorFilter = ColorFilter.colorMatrix(ColorMatrix())
            )
            when (state) {
                is LoadingState.Loading -> {
                    Text(state.infoText, textAlign = TextAlign.Center)
                    if (state.progress == null) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    } else {
                        val animatedProgress by animateFloatAsState(state.progress, label = "Loading progress", animationSpec = spring(dampingRatio = 2F))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    }
                }

                LoadingState.Error -> {
                    Text("Zdá se, ža vaše jizdní řády uložené v zařízení jsou poškozené!")
                    TextButton(
                        onClick = {
                            onEvent(LoadingEvent.DownloadDataIfError)
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Stáhnout nové JŘ")
                    }
                }

                LoadingState.Offline -> {
                    Text("Na stažení jizdních řádů je potřeba připojení k internetu!")
                }
            }
        }
    }
}