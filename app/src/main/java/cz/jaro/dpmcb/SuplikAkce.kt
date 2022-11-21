package cz.jaro.dpmcb

import android.content.Intent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ramcosta.composedestinations.navigation.navigate
import cz.jaro.dpmcb.data.helperclasses.Datum
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.destinations.MapaScreenDestination
import cz.jaro.dpmcb.ui.destinations.SpojeniScreenDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

enum class SuplikAkce(
    @StringRes val jmeno: Int,
    val icon: ImageVector,
    val multiselect: Boolean,
    val onClick: (navController: NavHostController, zavrit: () -> Unit, activity: MainActivity) -> Unit,
) {
    Spojeni(
        R.string.vyhledat_spojeni,
        Icons.Default.Timeline,
        true,
        onClick = { navController, zavrit, _ ->
            navController.navigate(
                SpojeniScreenDestination()
            )
            zavrit()
        }
    ),
    JizdniRady(
        R.string.jizdni_rady,
        Icons.Default.FormatListNumbered,
        true,
        onClick = { navController, zavrit, _ ->

            navController.navigate(
                VybiratorScreenDestination(
                    typ = TypAdapteru.LINKY,
                    cisloLinky = -1,
                    zastavka = null,
                )
            )
            zavrit()
        }
    ),
    Odjezdy(
        R.string.odjezdy,
        Icons.Default.DepartureBoard,
        true,
        onClick = { navController, zavrit, _ ->

            navController.navigate(
                VybiratorScreenDestination(
                    typ = TypAdapteru.ZASTAVKY,
                    cisloLinky = -1,
                    zastavka = null
                )
            )
            zavrit()
        }
    ),
    Mapa(
        R.string.mapa_linek,
        Icons.Default.Map,
        true,
        onClick = { navController, zavrit, _ ->

            navController.navigate(
                MapaScreenDestination()
            )
            zavrit()
        }
    ),
    Datum(
        R.string.zmena_data,
        Icons.Default.Today,
        false,
        onClick = { _, _, activity ->

            activity.lifecycleScope.launch {

                MaterialAlertDialogBuilder(activity).apply {
                    setTitle("Změna data")

                    val ll = LinearLayout(context)

                    val dp = android.widget.DatePicker(context)
                    //dp.maxDate = Calendar.getInstance().apply { set(3000, 12, 30) }.timeInMillis
                    dp.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    dp.updateLayoutParams<LinearLayout.LayoutParams> {
                        updateMargins(top = 16)
                    }

                    val repo = GlobalContext.get().get<cz.jaro.dpmcb.data.SpojeRepository>()

                    if (repo.datum.toInt() != 0)
                        dp.updateDate(
                            repo.datum.rok,
                            repo.datum.mesic - 1,
                            repo.datum.den
                        )

                    ll.addView(dp)

                    setView(ll)

                    setPositiveButton("Zvolit") { dialog, _ ->
                        dialog.cancel()



                        repo.upravitDatum(Datum(dp.dayOfMonth, dp.month + 1, dp.year))
                    }
                    show()
                }
            }
        }
    ),
    Nastaveni(
        R.string.nastaveni,
        Icons.Default.Settings,
        false,
        onClick = { _, _, activity ->

            val intent = Intent(activity, NastaveniActivity::class.java)
            activity.startActivity(intent)
        }
    ),
    ZpetnaVazba(
        R.string.zpetna_vazba,
        Icons.Default.Stars,
        false,
        onClick = { _, zavrit, _ ->

            zavrit()
        }
    ),
    Vypnout(
        R.string.vypnout_aplikaci,
        Icons.Default.PowerSettingsNew,
        false,
        onClick = { _, zavrit, activity ->

            activity.finish()

            zavrit()
        }
    );
}
