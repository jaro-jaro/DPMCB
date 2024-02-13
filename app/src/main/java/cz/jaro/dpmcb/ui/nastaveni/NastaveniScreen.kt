package cz.jaro.dpmcb.ui.nastaveni

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
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
fun Nastaveni(
    finish: () -> Unit,
) {

    val ctx = LocalContext.current

    val viewModel: NastaveniViewModel = koinViewModel {
        parametersOf(
            NastaveniViewModel.Parameters(
                startActivity = { intent: Intent ->
                    ctx.startActivity(intent)
                },
                loadingActivityIntent = Intent(ctx, LoadingActivity::class.java),
                jsteOfflineToast = {
                    Toast.makeText(ctx, "Jste offline!", Toast.LENGTH_SHORT).show()
                },
                navigateBack = finish
            )
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    NastaveniScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NastaveniScreen(
    onEvent: (NastaveniEvent) -> Unit,
    state: NastaveniState,
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
                                onEvent(NastaveniEvent.NavigateBack)
                            }
                        ) {
                            UtilFunctions.IconWithTooltip(Icons.Default.ArrowBack, "Zpět")
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
                        checked = state.nastaveni.dmPodleSystemu,
                        onCheckedChange = { value ->
                            onEvent(NastaveniEvent.UpravitNastaveni {
                                it.copy(dmPodleSystemu = value)
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
                        checked = if (state.nastaveni.dmPodleSystemu) isSystemInDarkTheme() else state.nastaveni.dm,
                        onCheckedChange = { value ->
                            onEvent(NastaveniEvent.UpravitNastaveni {
                                it.copy(dm = value)
                            })
                        },
                        enabled = !state.nastaveni.dmPodleSystemu
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
                    val selectedOption by remember(state.nastaveni.dynamickeBarvy, state.nastaveni.tema) {
                        derivedStateOf {
                            when {
                                dynamicColorsSupported && state.nastaveni.dynamickeBarvy -> options.first()
                                else -> state.nastaveni.tema.jmeno
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
                                        onEvent(NastaveniEvent.UpravitNastaveni { nastaveni ->
                                            when {
                                                dynamicColorsSupported && i == 0 -> nastaveni.copy(dynamickeBarvy = true)
                                                dynamicColorsSupported -> nastaveni.copy(tema = Theme.entries[i - 1], dynamickeBarvy = false)
                                                else -> nastaveni.copy(tema = Theme.entries[i], dynamickeBarvy = false)
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
                        checked = !state.nastaveni.autoOnline,
                        onCheckedChange = { value ->
                            onEvent(NastaveniEvent.UpravitNastaveni {
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
                        checked = state.nastaveni.kontrolaAktualizaci,
                        onCheckedChange = { value ->
                            onEvent(NastaveniEvent.UpravitNastaveni {
                                it.copy(kontrolaAktualizaci = value)
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
                            onClick = { onEvent(NastaveniEvent.AktualizovatAplikaci) }
                        ) {
                            Text("Aktualizovat aplikaci")
                        }
                    }
                    Box(
                        Modifier.weight(1F),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = { onEvent(NastaveniEvent.AktualizovatData) }
                        ) {
                            Text("Aktualizovat data")
                        }
                    }
                }
                textItem("Aktuální verze dat: ${state.metaVerzeDat}.${state.verzeDat}")
                textItem("Aktuální verze aplikace: ${state.verze}")
                textItem("2021-${LocalDate.now().year} RO studios, člen skupiny JARO")
                textItem("2019-${LocalDate.now().year} JARO")
                textItem("Za zobrazená data neručíme")

                item {
                    Text("Simulate crash...", Modifier.clickable {
                        throw RuntimeException("Test exception")
                    }, fontSize = 10.sp)
                }
            }
        }
    }
}