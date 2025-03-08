package cz.jaro.dpmcb.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import cz.jaro.dpmcb.data.AppState
import cz.jaro.dpmcb.data.Settings
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.isDebug
import cz.jaro.dpmcb.data.helperclasses.rowItem
import cz.jaro.dpmcb.data.helperclasses.superNavigateFunction
import cz.jaro.dpmcb.data.helperclasses.textItem
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.viewModel
import cz.jaro.dpmcb.ui.common.openWebsiteLauncher
import cz.jaro.dpmcb.ui.main.DrawerAction
import cz.jaro.dpmcb.ui.main.Navigator
import cz.jaro.dpmcb.ui.main.Route
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.Theme
import cz.jaro.dpmcb.ui.theme.areDynamicColorsSupported
import org.jetbrains.compose.ui.tooling.preview.Preview

@Suppress("unused")
@Composable
fun Settings(
    args: Route.Settings,
    navigator: Navigator,
    superNavController: NavHostController,
    viewModel: SettingsViewModel = viewModel(),
) {
    AppState.title = "Nastavení"
    AppState.selected = DrawerAction.Settings

    LaunchedEffect(Unit) {
        viewModel.superNavigate = superNavController.superNavigateFunction
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

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
        .padding(horizontal = 16.dp)
) {
    if (state.settings != null) settings(state.settings, onEvent)
    else rowItem(
        Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }

    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var loading by rememberSaveable { mutableStateOf(null as String?) }

        if (loading != null) AlertDialog(
            onDismissRequest = { loading = null },
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Text(loading!!, Modifier.padding(start = 8.dp))
                }
            },
        )
        Box(
            Modifier.weight(1F),
            contentAlignment = Alignment.CenterStart
        ) {
            Button(
                onClick = { onEvent(SettingsEvent.UpdateApp { loading = it }) },
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
    textItem("Aktuální verze aplikace: ${state.version}${if (isDebug) "-DEBUG" else ""}")

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
    item {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.settings(
    settings: Settings,
    onEvent: (SettingsEvent) -> Unit,
) {
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Určit tmavý režim podle systému", Modifier.weight(1F))

        Switch(
            checked = settings.dmAsSystem,
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
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Tmavý režim", Modifier.weight(1F))

        Switch(
            checked = if (settings.dmAsSystem) isSystemInDarkTheme() else settings.dm,
            onCheckedChange = { value ->
                onEvent(SettingsEvent.EditSettings {
                    it.copy(dm = value)
                })
            },
            enabled = !settings.dmAsSystem
        )
    }
    rowItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dynamicColorsSupported = areDynamicColorsSupported()
        val options = remember {
            buildList {
                if (dynamicColorsSupported) add("Dynamické")
                addAll(Theme.entries.map { it.jmeno })
            }
        }
        var expanded by remember { mutableStateOf(false) }
        val selectedOption by remember(settings.dynamicColors, settings.theme) {
            derivedStateOf {
                when {
                    dynamicColorsSupported && settings.dynamicColors -> options.first()
                    else -> settings.theme.jmeno
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
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Automaticky zakázat připojení k internetu po zapnutí aplikace", Modifier.weight(1F))

        Switch(
            checked = !settings.autoOnline,
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
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Provádět kontrolu dostupnosti aktualizací při startu aplikace", Modifier.weight(1F))

        Switch(
            checked = settings.checkForUpdates,
            onCheckedChange = { value ->
                onEvent(SettingsEvent.EditSettings {
                    it.copy(checkForUpdates = value)
                })
            },
        )
    }

    item {
        var value by remember { mutableStateOf(settings.recentBusesCount.toString()) }

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            value = value,
            onValueChange = {
                value = it
                onEvent(SettingsEvent.EditSettings {
                    it.copy(recentBusesCount = value.toIntOrNull() ?: it.recentBusesCount)
                })
            },
            isError = value != settings.recentBusesCount.toString(),
            label = { Text("Počet uložených nedávno navštívených spojů") },
            supportingText = { Text("Nastavte 0 pro úplné vypnutí funkce") },
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            )
        )
    }
}

@Composable
private fun TextWithLink(text: AnnotatedString) {
    val openWebsite = openWebsiteLauncher
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { pos ->
                layoutResult?.let { layoutResult ->
                    val offset = layoutResult.getOffsetForPosition(pos)
                    text.getStringAnnotations(tag = "link", start = offset, end = offset).firstOrNull()?.let { stringRange ->
                        openWebsite(stringRange.item)
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
        Surface {
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
}