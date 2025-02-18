package cz.jaro.dpmcb.ui.settings

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.rowItem
import cz.jaro.dpmcb.data.helperclasses.superNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.textItem
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.Theme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Suppress("unused")
@Composable
fun Settings(
    args: Route.Settings,
    navController: NavHostController,
    superNavController: NavHostController,
    viewModel: SettingsViewModel = run {
        val ctx = LocalContext.current

        koinViewModel {
            parametersOf(
                SettingsViewModel.Parameters(
                    startActivity = { intent: Intent ->
                        ctx.startActivity(intent)
                    },
                    youAreOfflineToast = {
                        Toast.makeText(ctx, "Jste offline!", Toast.LENGTH_SHORT).show()
                    },
                )
            )
        }
    },
) {
    LaunchedEffect(Unit) {
        viewModel.superNavigate = superNavController.superNavigateFunction
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    App.title = R.string.settings
    App.selected = DrawerAction.Settings

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun SettingsScreen(
    onEvent: (SettingsEvent) -> Unit,
    state: SettingsState,
) = LazyColumn(
    modifier = Modifier
        .fillMaxSize()
) {
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Určit tmavý režim podle systému", Modifier.weight(1F))

        Switch(
            checked = state.settings.dmAsSystem,
            onCheckedChange = { value ->
                onEvent(SettingsEvent.EditSettings {
                    it.copy(dmAsSystem = value)
                })
            },
        )
    }
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Tmavý režim", Modifier.weight(1F))

        Switch(
            checked = if (state.settings.dmAsSystem) isSystemInDarkTheme() else state.settings.dm,
            onCheckedChange = { value ->
                onEvent(SettingsEvent.EditSettings {
                    it.copy(dm = value)
                })
            },
            enabled = !state.settings.dmAsSystem
        )
    }
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dynamicColorsSupported = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
        val options = remember {
            buildList {
                if (dynamicColorsSupported) add("Dynamické")
                addAll(Theme.entries.map { it.jmeno })
            }
        }
        var expanded by remember { mutableStateOf(false) }
        val selectedOption by remember(state.settings.dynamicColors, state.settings.theme) {
            derivedStateOf {
                when {
                    dynamicColorsSupported && state.settings.dynamicColors -> options.first()
                    else -> state.settings.theme.jmeno
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            TextField(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                readOnly = true,
                value = selectedOption,
                onValueChange = {},
                label = { Text("Téma aplikace") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEachIndexed { i, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onEvent(SettingsEvent.EditSettings { settings ->
                                when {
                                    dynamicColorsSupported && i == 0 -> settings.copy(dynamicColors = true)
                                    dynamicColorsSupported -> settings.copy(theme = Theme.entries[i - 1], dynamicColors = false)
                                    else -> settings.copy(theme = Theme.entries[i], dynamicColors = false)
                                }
                            })
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Automaticky zakázat připojení k internetu po zapnutí aplikace", Modifier.weight(1F))

        Switch(
            checked = !state.settings.autoOnline,
            onCheckedChange = { value ->
                onEvent(SettingsEvent.EditSettings {
                    it.copy(autoOnline = !value)
                })
            },
        )
    }
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Provádět kontrolu dostupnosti aktualizací při startu aplikace", Modifier.weight(1F))

        Switch(
            checked = state.settings.checkForUpdates,
            onCheckedChange = { value ->
                onEvent(SettingsEvent.EditSettings {
                    it.copy(checkForUpdates = value)
                })
            },
        )
    }

    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1F),
            contentAlignment = Alignment.CenterStart
        ) {
            Button(
                onClick = { onEvent(SettingsEvent.UpdateApp) },
                enabled = state.isOnline,
            ) {
                Text("Aktualizovat aplikaci")
            }
        }
        Box(
            Modifier.weight(1F),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = { onEvent(SettingsEvent.UpdateData) },
                enabled = state.isOnline,
            ) {
                Text("Aktualizovat data")
            }
        }
    }
    textItem("Aktuální verze dat: ${state.dataMetaVersion}.${state.dataVersion}")
    textItem("Aktuální verze aplikace: ${state.version}${if (BuildConfig.DEBUG) "-DEBUG" else ""}")

    textItem("")

    item {
        TextWithLink(buildAnnotatedString {
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
            ) {
                append("Zdroj dat: ")
            }
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.primary)
            ) {
                withAnnotation(
                    tag = "link",
                    annotation = "https://portal.cisjr.cz/IDS/Search.aspx?param=cbcz"
                ) {
                    append("CIS JŘ (CHAPS)")
                }
            }
        })
    }
    item {
        TextWithLink(buildAnnotatedString {
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
            ) {
                append("Zdroj aktuálních dat: ")
            }
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.primary)
            ) {
                withAnnotation(
                    tag = "link",
                    annotation = "https://mpvnet.cz/jikord/map"
                ) {
                    append("MPV (CHAPS)")
                }
            }
        })
    }
    item {
        TextWithLink(buildAnnotatedString {
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
            ) {
                append("Zdroj informací o nasazení na kurzech: ")
            }
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.primary)
            ) {
                withAnnotation(
                    tag = "link",
                    annotation = "https://seznam-autobusu.cz/vypravenost/mhd-cb"
                ) {
                    append("Seznam autobusů")
                }
            }
        })
    }
    textItem("Veškerá data o kurzech jsou neoficiální a proto za ně neručíme")

    textItem("")

    textItem("2021-${SystemClock.todayHere().year} RO studios, člen skupiny JARO")
    textItem("2019-${SystemClock.todayHere().year} JARO")

    item {
        Text("Simulate crash...", Modifier.clickable {
            throw RuntimeException("Test exception")
        }, fontSize = 10.sp)
    }
}

@Composable
private fun TextWithLink(text: AnnotatedString) {
    val context = LocalContext.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { pos ->
                layoutResult?.let { layoutResult ->
                    val offset = layoutResult.getOffsetForPosition(pos)
                    text.getStringAnnotations(tag = "link", start = offset, end = offset).firstOrNull()?.let { stringRange ->
                        CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                            .launchUrl(context, stringRange.item.toUri())
                    }
                }
            }
        },
        onTextLayout = {
            layoutResult = it
        }
    )
}

@Preview
@Composable
private fun SettingsPreview() {
    val settings = Settings()
    DPMCBTheme(settings) {
        SettingsScreen(
            onEvent = {},
            state = SettingsState(
                settings = settings,
                version = "1.0",
                dataVersion = 5,
                dataMetaVersion = 1,
                isOnline = false,
            )
        )
    }
}