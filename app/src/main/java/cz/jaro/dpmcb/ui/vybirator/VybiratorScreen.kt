package cz.jaro.dpmcb.ui.vybirator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import cz.jaro.dpmcb.FakeNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.UiEvent
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

data class Vysledek(val value: String, val typAdapteru: TypAdapteru) : java.io.Serializable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Destination
@Composable
fun VybiratorScreen(
    navigator: DestinationsNavigator,
    resultNavigator: ResultBackNavigator<Vysledek>,
    typ: TypAdapteru,
    cisloLinky: Int = -1,
    zastavka: String? = null,
    viewModel: VybiratorViewModel = koinViewModel {
        ParametersHolder(mutableListOf(typ, cisloLinky, zastavka, resultNavigator))
    },
) {

    App.title = when (typ) {
        TypAdapteru.ZASTAVKY -> R.string.vyberte_zastavku
        TypAdapteru.LINKY -> R.string.vyberte_linku
        TypAdapteru.ZASTAVKY_LINKY -> R.string.vyberte_zastavku
        TypAdapteru.PRISTI_ZASTAVKA -> R.string.vyberte_dalsi_zastávku
        TypAdapteru.ZASTAVKY_ZPET_1 -> R.string.vyberte_linku
        TypAdapteru.ZASTAVKA_ZPET_2 -> R.string.vyberte_zastavku
        TypAdapteru.LINKA_ZPET -> R.string.vyberte_linku
        TypAdapteru.ZASTAVKA_ZPET -> R.string.vyberte_zastavku
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigovat -> navigator.navigate(event.kam)
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(text = viewModel.state.info)
        val focusNaTextField = remember { FocusRequester() }
        TextField(
            value = viewModel.state.hledani,
            onValueChange = { viewModel.poslatEvent(VybiratorEvent.NapsalNeco(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .focusRequester(focusNaTextField)
                .onKeyEvent {
                    if (it.key == Key.Enter) {
                        viewModel.poslatEvent(VybiratorEvent.KliklEnter)
                    }
                    return@onKeyEvent it.key == Key.Enter
                },
            label = { Text("Vyhledávání") },
            keyboardOptions = KeyboardOptions.Default.copy(
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None,
                keyboardType = when (typ) {
                    TypAdapteru.ZASTAVKY -> KeyboardType.Text
                    TypAdapteru.LINKY -> KeyboardType.Number
                    TypAdapteru.ZASTAVKY_LINKY -> KeyboardType.Text
                    TypAdapteru.PRISTI_ZASTAVKA -> KeyboardType.Text
                    TypAdapteru.ZASTAVKY_ZPET_1 -> KeyboardType.Text
                    TypAdapteru.ZASTAVKA_ZPET_2 -> KeyboardType.Text
                    TypAdapteru.LINKA_ZPET -> KeyboardType.Number
                    TypAdapteru.ZASTAVKA_ZPET -> KeyboardType.Text
                },
                imeAction = when (typ) {
                    TypAdapteru.ZASTAVKY -> ImeAction.Search
                    TypAdapteru.LINKY -> ImeAction.Next
                    TypAdapteru.ZASTAVKY_LINKY -> ImeAction.Next
                    TypAdapteru.PRISTI_ZASTAVKA -> ImeAction.Search
                    TypAdapteru.ZASTAVKY_ZPET_1 -> ImeAction.Done
                    TypAdapteru.ZASTAVKA_ZPET_2 -> ImeAction.Done
                    TypAdapteru.LINKA_ZPET -> ImeAction.Done
                    TypAdapteru.ZASTAVKA_ZPET -> ImeAction.Done
                },
            ),
            keyboardActions = KeyboardActions(
                onAny = {
                    viewModel.poslatEvent(VybiratorEvent.KliklEnter)
                }
            ),
            singleLine = true,
            maxLines = 1,
        )

        val keyboard = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            focusNaTextField.requestFocus()
            delay(100)
            keyboard!!.show()
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(viewModel.state.seznam.toList()) { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    modifier = Modifier
                        .padding(all = 8.dp),
                    onClick = {
                        viewModel.poslatEvent(VybiratorEvent.KliklNaSeznam(item))
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun VybiratorPreview() {
    MaterialTheme {
        Surface {
            VybiratorScreen(
                navigator = FakeNavigator,
                resultNavigator = FakeNavigator,
                typ = TypAdapteru.ZASTAVKY
            )
        }
    }
}
