package cz.jaro.dpmcb.ui.spojeni

import android.widget.TimePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibleForward
import androidx.compose.material.icons.filled.NoTransfer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.cas
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.IconWithTooltip
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import cz.jaro.dpmcb.ui.vybirator.Vysledek
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun SpojeniScreen(
    navigator: DestinationsNavigator,
    viewModel: SpojeniViewModel = koinViewModel(),
    resultRecipient: ResultRecipient<VybiratorScreenDestination, Vysledek>,
) {
    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {}
            is NavResult.Value -> {
                viewModel.vybralZastavku(result.value.typAdapteru == TypAdapteru.ZASTAVKY_ZPET_1, result.value.value)
            }
        }
    }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigovat -> {
                    navigator.navigate(event.kam)
                }

                is UiEvent.Zkopirovat -> {
                    clipboardManager.setText(AnnotatedString(event.text))
                }

                else -> {}
            }
        }
    }

    val state by viewModel.nastaveni.collectAsState()
    val historie by viewModel.historie.collectAsState()

    App.title = R.string.vyhledat_spojeni

    var nastaveniVidim by remember { mutableStateOf(false) }
    var casovatorVidim by remember { mutableStateOf(false) }

    if (nastaveniVidim) AlertDialog(
        onDismissRequest = { nastaveniVidim = false },
        confirmButton = {
            TextButton(onClick = {
                nastaveniVidim = false
            }) {
                Text("OK")
            }
        },
        title = { Text(text = "Nastavení vyhledávání spojení") },
        text = {
            Column(Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Pouze nízkopodlažní")

                    Switch(
                        checked = state.jenNizkopodlazni,
                        onCheckedChange = { np ->
                            viewModel.upravitNastaveni { it.copy(jenNizkopodlazni = np) }
                        },
                        thumbContent = {
                            IconWithTooltip(Icons.Default.AccessibleForward, contentDescription = "Sprintující vozíček", modifier = Modifier.padding(2.dp))
                        })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Pouze přímá spojení")

                    Switch(
                        checked = state.jenPrima,
                        onCheckedChange = { prima ->
                            viewModel.upravitNastaveni { it.copy(jenPrima = prima) }
                        },
                        thumbContent = {
                            IconWithTooltip(Icons.Default.NoTransfer, contentDescription = "Nepřestupovací ikonka", modifier = Modifier.padding(2.dp))
                        }
                    )
                }
            }
        }
    )
    if (casovatorVidim) AlertDialog(
        onDismissRequest = { casovatorVidim = false },
        confirmButton = {
            TextButton(onClick = {
                casovatorVidim = false
            }) {
                Text("OK")
            }
        },
        title = { Text(text = "Změna času") },
        text = {
            AndroidView(factory = { context ->
                TimePicker(context).apply {
                    setIs24HourView(true)

                    setOnTimeChangedListener { _, h, min ->
                        viewModel.upravitNastaveni {
                            it.copy(cas = h cas min)
                        }
                    }
                }
            }, update = {
                it.hour = state.cas.h
                it.minute = state.cas.min
            }, modifier = Modifier.padding(16.dp))
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            val focusRequester = LocalFocusManager.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.prohazujeZastavky()
                    },
                    //Modifier.padding(all = 4.dp)
                ) {
                    IconWithTooltip(Icons.Default.SwapVert, null)
                }
                Column {
                    OutlinedTextField(
                        value = state.start,
                        onValueChange = {},
                        Modifier.onFocusEvent {
                            if (!it.hasFocus) return@onFocusEvent
                            viewModel.vybratZastavku(true)
                            focusRequester.clearFocus()
                        },
                        label = {
                            Text("Odkud")
                        }
                    )
                    OutlinedTextField(
                        value = state.cil,
                        onValueChange = {},
                        Modifier
                            .onFocusEvent {
                                if (!it.hasFocus) return@onFocusEvent
                                viewModel.vybratZastavku(false)
                                focusRequester.clearFocus()
                            },
                        label = {
                            Text("Kam")
                        }
                    )
                }
                IconButton(
                    onClick = {},
                    enabled = false
                    //Modifier.padding(all = 4.dp)
                ) {
                    IconWithTooltip(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.surface)
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedIconButton(
                    onClick = {
                        nastaveniVidim = true
                    }
                ) {
                    IconWithTooltip(Icons.Default.Settings, "Nastavení")
                }
                Button(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = {
                        viewModel.vyhledat()
                    },
                ) {
                    Text("Vyhledat Spojení")
                }
                OutlinedIconButton(
                    onClick = {
                        casovatorVidim = true
                    }
                ) {
                    IconWithTooltip(Icons.Default.Schedule, "Změnit čas")
                }
            }
        }

        item {
            Text(text = "Historie vyhledávání:", style = MaterialTheme.typography.titleMedium)
        }

        if (historie.isEmpty()) item {
            Text(text = "Zatím nic :(")
        }

        itemsIndexed(historie) { i, (start, cil) ->
            ListItem(
                headlineText = {
                    Text(text = "$start -> $cil")
                },
                modifier = Modifier.clickable {
                    viewModel.vyhledatZHistorie(i)
                },
            )
        }
    }
}
