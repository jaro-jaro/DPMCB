package cz.jaro.dpmcb.ui.nastaveni

import android.content.Intent
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.data.Nastaveni
import cz.jaro.dpmcb.data.helperclasses.MutateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.rowItem
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.textItem
import cz.jaro.dpmcb.ui.loading.LoadingViewModel
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
                finish = finish,
                loadingActivityIntent = Intent(ctx, LoadingActivity::class.java),
                jsteOfflineToast = {
                    Toast.makeText(ctx, "Jste offline!", Toast.LENGTH_SHORT).show()
                },
            )
        )
    }

    val nastaveni by viewModel.nastaveni.collectAsStateWithLifecycle()

    NastaveniScreen(
        navigateBack = finish,
        nastaveni = nastaveni,
        aktualizovatAplikaci = viewModel::aktualizovatAplikaci,
        aktualizovatData = viewModel::aktualizovatData,
        upravit = viewModel.upravitNastaveni,
        verze = BuildConfig.VERSION_NAME,
        verzeDat = viewModel.verze,
        metaVerzeDat = LoadingViewModel.META_VERZE_DAT,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NastaveniScreen(
    navigateBack: () -> Unit,
    nastaveni: Nastaveni,
    aktualizovatAplikaci: () -> Unit,
    aktualizovatData: () -> Unit,
    upravit: MutateFunction<Nastaveni>,
    verze: String,
    verzeDat: Int,
    metaVerzeDat: Int,
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
                            onClick = navigateBack
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
                        checked = nastaveni.dmPodleSystemu,
                        onCheckedChange = { value ->
                            upravit {
                                it.copy(dmPodleSystemu = value)
                            }
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
                        checked = if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm,
                        onCheckedChange = { value ->
                            upravit {
                                it.copy(dm = value)
                            }
                        },
                        enabled = !nastaveni.dmPodleSystemu
                    )
                }
                rowItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Automaticky zakázat připojení k internetu po zapnutí aplikace", Modifier.weight(1F))

                    Switch(
                        checked = !nastaveni.autoOnline,
                        onCheckedChange = { value ->
                            upravit {
                                it.copy(autoOnline = !value)
                            }
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
                        checked = nastaveni.kontrolaAktualizaci,
                        onCheckedChange = { value ->
                            upravit {
                                it.copy(kontrolaAktualizaci = value)
                            }
                        },
                    )
                }
                rowItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Zapamatovat si volbu Zobrazit nízkopodlažnosti v JŘ", Modifier.weight(1F))

                    Switch(
                        checked = nastaveni.zachovavatNizkopodlaznost,
                        onCheckedChange = { value ->
                            upravit {
                                it.copy(zachovavatNizkopodlaznost = value)
                            }
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
                            onClick = aktualizovatAplikaci
                        ) {
                            Text("Aktualizovat aplikaci")
                        }
                    }
                    Box(
                        Modifier.weight(1F),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = aktualizovatData
                        ) {
                            Text("Aktualizovat data")
                        }
                    }
                }
                textItem("Aktuální verze dat: $metaVerzeDat.$verzeDat")
                textItem("Aktuální verze aplikace: $verze")
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