package cz.jaro.dpmcb.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.textItem
import cz.jaro.dpmcb.ui.theme.Theme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate

@Composable
fun Settings(
    finish: () -> Unit,
) {

    val ctx = LocalContext.current

    val viewModel: SettingsViewModel = koinViewModel {
        parametersOf(
            SettingsViewModel.Parameters(
                startActivity = { intent: Intent ->
                    ctx.startActivity(intent)
                },
                loadingActivityIntent = Intent(ctx, LoadingActivity::class.java),
                youAreOfflineToast = {
                    Toast.makeText(ctx, "Jste offline!", Toast.LENGTH_SHORT).show()
                },
                navigateBack = finish
            )
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalTextApi::class)
@Composable
fun SettingsScreen(
    onEvent: (SettingsEvent) -> Unit,
    state: SettingsState,
) {
    Surface {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Nastavení")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                onEvent(SettingsEvent.NavigateBack)
                            }
                        ) {
                            UtilFunctions.IconWithTooltip(Icons.AutoMirrored.Filled.ArrowBack, "Zpět")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
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
                                .menuAnchor()
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

//                rowItem(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    Text("Speciální režim", Modifier.weight(1F))
//
//                    Switch(
//                        checked = state.settings.special,
//                        onCheckedChange = { value ->
//                            onEvent(SettingsEvent.EditSettings {
//                                it.copy(special = value)
//                            })
//                        },
//                    )
//                }

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
                            onClick = { onEvent(SettingsEvent.UpdateApp) }
                        ) {
                            Text("Aktualizovat aplikaci")
                        }
                    }
                    Box(
                        Modifier.weight(1F),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = { onEvent(SettingsEvent.UpdateData) }
                        ) {
                            Text("Aktualizovat data")
                        }
                    }
                }
                textItem("Aktuální verze dat: ${state.dataMetaVersion}.${state.dataVersion}")
                textItem("Aktuální verze aplikace: ${state.version}")

                textItem("")

                item {
                    val context = LocalContext.current
                    val text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
                        ) {
                            append("Zdroj dat: ")
                        }
                        withStyle(
                            style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                        ) {
                            withAnnotation(
                                tag = "cis",
                                annotation = "https://portal.cisjr.cz/IDS/Search.aspx?param=cbcz"
                            ) {
                                append("CIS JŘ")
                            }
                        }
                    }
                    ClickableText(
                        text = text,
                        style = LocalTextStyle.current,
                        onClick = { offset ->
                            text.getStringAnnotations(tag = "cis", start = offset, end = offset).firstOrNull()?.let { stringRange ->
                                CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                    .launchUrl(context, Uri.parse(stringRange.item))
                            }
                        }
                    )
                }
                textItem("Veškerá data o kurzech jsou neoficiální a proto za ně neručíme")

                textItem("")

                textItem("2021-${LocalDate.now().year} RO studios, člen skupiny JARO")
                textItem("2019-${LocalDate.now().year} JARO")

                item {
                    Text("Simulate crash...", Modifier.clickable {
                        throw RuntimeException("Test exception")
                    }, fontSize = 10.sp)
                }
            }
        }
    }
}