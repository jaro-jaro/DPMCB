package cz.jaro.dpmcb.ui.spojeni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.ui.UiEvent
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import cz.jaro.dpmcb.ui.vybirator.Vysledek
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@RootNavGraph(start = true)
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
                viewModel.poslatEvent(SpojeniEvent.VybralZastavku(result.value.v.first, result.value.v.second))
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

    val state by viewModel.state.collectAsState()

    App.title = R.string.vyhledat_spojeni

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val focusRequester = LocalFocusManager.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.poslatEvent(SpojeniEvent.ProhazujeZastavky)
                },
                //Modifier.padding(all = 4.dp)
            ) {
                Icon(Icons.Default.SwapVert, null)
            }
            Column {
                OutlinedTextField(
                    value = state.start,
                    onValueChange = {},
                    Modifier.onFocusEvent {
                        if (!it.hasFocus) return@onFocusEvent
                        viewModel.poslatEvent(SpojeniEvent.ChceVybratZastavku(true))
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
                            viewModel.poslatEvent(SpojeniEvent.ChceVybratZastavku(false))
                            focusRequester.clearFocus()
                        },
                    label = {
                        Text("Kam")
                    }
                )
            }
            IconButton(
                onClick = {},
                //Modifier.padding(all = 4.dp)
            ) {
                Icon(Icons.Default.SwapVert, null, tint = MaterialTheme.colorScheme.surface)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedIconButton(
                onClick = {
                }
            ) {
                Icon(Icons.Default.Settings, "Nstavení")
            }
            Button(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    viewModel.poslatEvent(SpojeniEvent.Vyhledat)
                },
            ) {
                Text("Vyhledat Spojení")
            }
            OutlinedIconButton(
                onClick = {
                }
            ) {
                Icon(Icons.Default.Schedule, null)
            }
        }
    }
}
