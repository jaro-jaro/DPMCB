package cz.jaro.dpmcb

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cz.jaro.dpmcb.data.App
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            val keyboardController = LocalSoftwareKeyboardController.current!!

            DPMCBTheme {
                val drawerState = rememberDrawerState(DrawerValue.Open) {
                    keyboardController.hide()

                    true
                }
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                var vybrano by remember { mutableStateOf(SuplikAkce.Spojeni) }

                Scaffold(
                    topBar = {
                        // IconButtony
                        TopAppBar(title = {
                            Text(stringResource(App.title))
                        },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.apply {
                                                keyboardController.hide()
                                                if (isClosed) open() else close()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Otevřít"
                                    )
                                }
                            },
                            actions = {
                                // IconButtony
                            })
                    },
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .padding(paddingValues)
                    ) {
                        ModalNavigationDrawer(
                            drawerContent = {
                                ModalDrawerSheet {
                                    SuplikAkce.values().forEach {
                                        NavigationDrawerItem(
                                            label = {
                                                Text(stringResource(it.jmeno))
                                            },
                                            icon = {
                                                Icon(it.icon, stringResource(it.jmeno))
                                            },
                                            selected = vybrano == it,
                                            onClick = {
                                                if (it.multiselect)
                                                    vybrano = it
                                                it.onClick(navController, { scope.launch { drawerState.close() } }, this@MainActivity)
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            },
                            drawerState = drawerState,
                            gesturesEnabled = vybrano != SuplikAkce.Mapa
                        ) {
                            // Screen content
                            Navigation(navController)
                        }
                    }
                }
            }

        }

        if (intent.getBooleanExtra("update", false)) {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(R.string.aktualizace_jr)
                setMessage(R.string.chcete_aktualizovat)
                setNegativeButton(R.string.ne) { dialog, _ -> dialog.cancel() }

                setPositiveButton(R.string.ano) { dialog, _ ->
                    dialog.cancel()

                    val intent = Intent(context, LoadingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
                    intent.putExtra("update", true)
                    startActivity(intent)
                }
                show()
            }
        }
    }
}
