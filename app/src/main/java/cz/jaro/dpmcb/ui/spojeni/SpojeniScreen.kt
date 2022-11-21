package cz.jaro.dpmcb.ui.spojeni

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.navigation.compose.rememberNavController
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

    val controller = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val focusRequester = LocalFocusManager.current
        TextField(value = state.start, onValueChange = {}, Modifier.onFocusEvent {
            if (!it.hasFocus) return@onFocusEvent
            viewModel.poslatEvent(SpojeniEvent.ChceVybratZastavku(true))
            focusRequester.clearFocus()
        })
        TextField(value = state.cil, onValueChange = {}, Modifier.onFocusEvent {
            if (!it.hasFocus) return@onFocusEvent
            viewModel.poslatEvent(SpojeniEvent.ChceVybratZastavku(false))
            focusRequester.clearFocus()
        })

        Button(
            modifier = Modifier.padding(16.dp),
            onClick = {
                viewModel.poslatEvent(SpojeniEvent.Vyhledat)
            },
        ) {
            Text("Vyhledat Spojení")
        }
    }
}
