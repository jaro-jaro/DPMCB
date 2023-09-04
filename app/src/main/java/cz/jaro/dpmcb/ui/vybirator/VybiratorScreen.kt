package cz.jaro.dpmcb.ui.vybirator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.App.Companion.title
import cz.jaro.dpmcb.data.App.Companion.vybrano
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.navigateFunction
import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.main.SuplikAkce
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Destination
@Composable
fun Vybirator(
    navigator: DestinationsNavigator,
    resultNavigator: ResultBackNavigator<Vysledek>,
    typ: TypVybiratoru,
    cisloLinky: Int = -1,
    zastavka: String? = null,
    viewModel: VybiratorViewModel = koinViewModel {
        parametersOf(VybiratorViewModel.Parameters(
            typ = typ,
            cisloLinky = cisloLinky,
            zastavka = zastavka,
            navigate = navigator.navigateFunction,
            navigateBack = { it: Vysledek -> resultNavigator.navigateBack(it) }
        ))
    },
) {
    title = when (typ) {
        TypVybiratoru.ZASTAVKY -> R.string.odjezdy
        TypVybiratoru.LINKY -> R.string.jizdni_rady
        TypVybiratoru.ZASTAVKY_LINKY -> R.string.jizdni_rady
        TypVybiratoru.PRISTI_ZASTAVKA -> R.string.jizdni_rady
        TypVybiratoru.ZASTAVKY_ZPET_1 -> R.string.vyhledat_spojeni
        TypVybiratoru.ZASTAVKA_ZPET_2 -> R.string.vyhledat_spojeni
        TypVybiratoru.LINKA_ZPET -> R.string.odjezdy
        TypVybiratoru.ZASTAVKA_ZPET -> R.string.odjezdy
    }
    vybrano = when (typ) {
        TypVybiratoru.ZASTAVKY -> SuplikAkce.Odjezdy
        TypVybiratoru.LINKY -> SuplikAkce.JizdniRady
        TypVybiratoru.ZASTAVKY_LINKY -> SuplikAkce.JizdniRady
        TypVybiratoru.PRISTI_ZASTAVKA -> SuplikAkce.JizdniRady
        TypVybiratoru.ZASTAVKY_ZPET_1 -> null
        TypVybiratoru.ZASTAVKA_ZPET_2 -> null
        TypVybiratoru.LINKA_ZPET -> SuplikAkce.Odjezdy
        TypVybiratoru.ZASTAVKA_ZPET -> SuplikAkce.Odjezdy
    }

    val hledani by viewModel.hledani.collectAsStateWithLifecycle()
    val seznam by viewModel.seznam.collectAsStateWithLifecycle()

    VybiratorScreen(
        typ = typ,
        info = viewModel.info,
        hledani = hledani,
        napsalNeco = viewModel::napsalNeco,
        kliklEnter = viewModel::kliklEnter,
        kliklNaVecZeSeznamu = viewModel::kliklNaVecZeSeznamu,
        seznam = seznam,
    )
}

@Composable
fun VybiratorScreen(
    typ: TypVybiratoru,
    info: String,
    hledani: String,
    napsalNeco: (String) -> Unit,
    kliklEnter: () -> Unit,
    kliklNaVecZeSeznamu: (String) -> Unit,
    seznam: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(text = info, Modifier.padding(horizontal = 16.dp))
        Text(
            text = stringResource(
                id = when (typ) {
                    TypVybiratoru.ZASTAVKY -> R.string.vyberte_zastavku
                    TypVybiratoru.LINKY -> R.string.vyberte_linku
                    TypVybiratoru.ZASTAVKY_LINKY -> R.string.vyberte_zastavku
                    TypVybiratoru.PRISTI_ZASTAVKA -> R.string.vyberte_dalsi_zastávku
                    TypVybiratoru.ZASTAVKY_ZPET_1 -> R.string.vyberte_linku
                    TypVybiratoru.ZASTAVKA_ZPET_2 -> R.string.vyberte_zastavku
                    TypVybiratoru.LINKA_ZPET -> R.string.vyberte_linku
                    TypVybiratoru.ZASTAVKA_ZPET -> R.string.vyberte_zastavku
                }
            ),
            Modifier.padding(all = 16.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
        )
        val focusNaTextField = remember { FocusRequester() }
        TextField(
            value = hledani,
            onValueChange = { napsalNeco(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .focusRequester(focusNaTextField)
                .onKeyEvent {
                    if (it.key == Key.Enter) {
                        kliklEnter()
                    }
                    return@onKeyEvent it.key == Key.Enter
                },
            label = { Text("Vyhledávání") },
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None,
                keyboardType = when (typ) {
                    TypVybiratoru.ZASTAVKY -> KeyboardType.Text
                    TypVybiratoru.LINKY -> KeyboardType.Number
                    TypVybiratoru.ZASTAVKY_LINKY -> KeyboardType.Text
                    TypVybiratoru.PRISTI_ZASTAVKA -> KeyboardType.Text
                    TypVybiratoru.ZASTAVKY_ZPET_1 -> KeyboardType.Text
                    TypVybiratoru.ZASTAVKA_ZPET_2 -> KeyboardType.Text
                    TypVybiratoru.LINKA_ZPET -> KeyboardType.Number
                    TypVybiratoru.ZASTAVKA_ZPET -> KeyboardType.Text
                },
                imeAction = when (typ) {
                    TypVybiratoru.ZASTAVKY -> ImeAction.Search
                    TypVybiratoru.LINKY -> ImeAction.Next
                    TypVybiratoru.ZASTAVKY_LINKY -> ImeAction.Next
                    TypVybiratoru.PRISTI_ZASTAVKA -> ImeAction.Search
                    TypVybiratoru.ZASTAVKY_ZPET_1 -> ImeAction.Done
                    TypVybiratoru.ZASTAVKA_ZPET_2 -> ImeAction.Done
                    TypVybiratoru.LINKA_ZPET -> ImeAction.Done
                    TypVybiratoru.ZASTAVKA_ZPET -> ImeAction.Done
                },
            ),
            keyboardActions = KeyboardActions {
                kliklEnter()
            },
            singleLine = true,
            maxLines = 1,
        )

        val softwareKeyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            focusNaTextField.requestFocus()
            delay(100)
            softwareKeyboardController!!.show()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            itemsIndexed(seznam) { i, item ->
                Surface(
                    onClick = {
                        kliklNaVecZeSeznamu(item)
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    shape = CircleShape,
                    color = if (i == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            kliklNaVecZeSeznamu(item)
                        },
                    )
                }
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
                typ = TypVybiratoru.ZASTAVKY,
                info = "",
                hledani = "u ko",
                napsalNeco = {},
                kliklEnter = {},
                kliklNaVecZeSeznamu = {},
                seznam = listOf("U Koníčka"),
            )
        }
    }
}
