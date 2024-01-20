package cz.jaro.dpmcb.ui.main

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.NastaveniActivity
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.ui.destinations.MapaDestination
import cz.jaro.dpmcb.ui.destinations.PraveJedouciDestination
import cz.jaro.dpmcb.ui.destinations.PrukazkaDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorDestination
import cz.jaro.dpmcb.ui.vybirator.TypVybiratoru
import kotlin.reflect.KClass


enum class SuplikAkce(
    @StringRes val jmeno: Int,
    val icon: ImageVector,
    val multiselect: Boolean,
    inline val onClick: (
        navigate: (direction: Direction) -> Unit,
        zavrit: () -> Unit,
        startActivity: (KClass<out Activity>) -> Unit,
    ) -> Unit,
) {
    /*Spojeni(
        R.string.vyhledat_spojeni,
        Icons.Default.Timeline,
        true,
        onClick = { navigate, zavrit, _ ->
            navigate(
                SpojeniScreenDestination()
            )
            zavrit()
        }
    ),*/
    Oblibene(
        jmeno = R.string.oblibena,
        icon = Icons.Default.Star,
        multiselect = true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                cz.jaro.dpmcb.ui.destinations.OblibeneDestination()
            )
            zavrit()
        }
    ),
    Odjezdy(
        jmeno = R.string.odjezdy,
        icon = Icons.Default.DepartureBoard,
        multiselect = true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                VybiratorDestination(
                    typ = TypVybiratoru.ZASTAVKY,
                    cisloLinky = -1,
                    zastavka = null
                )
            )
            zavrit()
        }
    ),
    PraveJedouci(
        jmeno = R.string.prave_jedouci,
        icon = Icons.Default.FastForward,
        multiselect = true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                PraveJedouciDestination()
            )
            zavrit()
        }
    ),
    JizdniRady(
        R.string.jizdni_rady,
        Icons.Default.FormatListNumbered,
        true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                VybiratorDestination(
                    typ = TypVybiratoru.LINKY,
                    cisloLinky = -1,
                    zastavka = null,
                )
            )
            zavrit()
        }
    ),
    NajitSpoj(
        R.string.spoj_podle_id,
        Icons.Default.Search,
        true,
        onClick = { _, _, _ -> }
    ),
    Kurz(
        R.string.detail_kurzu,
        Icons.Default.LinearScale,
        true,
        onClick = { _, _, _ -> }
    ),
    Prukazka(
        R.string.prukazka,
        icon = Icons.Default.QrCode,
        multiselect = true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                PrukazkaDestination()
            )
            zavrit()
        }
    ),
    Mapa(
        R.string.mapa_linek,
        Icons.Default.Map,
        true,
        onClick = { navigate, zavrit, _ ->

            navigate(
                MapaDestination()
            )
            zavrit()
        }
    ),
    Datum(
        R.string.typ_dne,
        Icons.Default.Today,
        false,
        onClick = { _, _, _ -> }
    ),
    Nastaveni(
        R.string.nastaveni,
        Icons.Default.Settings,
        false,
        onClick = { _, _, startActivity ->
            startActivity(NastaveniActivity::class as KClass<out Activity>)
        }
    ),
    ZpetnaVazba(
        R.string.zpetna_vazba,
        Icons.Default.Stars,
        false,
        onClick = { _, _, _ -> }
    ),
    Vypnout(
        R.string.vypnout_aplikaci,
        Icons.Default.PowerSettingsNew,
        false,
        onClick = { _, _, _ -> }
    )
}