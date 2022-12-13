package cz.jaro.dpmcb

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.navigation.navigate
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.destinations.MapaScreenDestination
import cz.jaro.dpmcb.ui.destinations.OblibeneScreenDestination
import cz.jaro.dpmcb.ui.destinations.VybiratorScreenDestination


enum class SuplikAkce(
    @StringRes val jmeno: Int,
    val icon: ImageVector,
    val multiselect: Boolean,
    val onClick: (navController: NavHostController, zavrit: () -> Unit, activity: MainActivity) -> Unit,
) {
    /*Spojeni(
        R.string.vyhledat_spojeni,
        Icons.Default.Timeline,
        true,
        onClick = { navController, zavrit, _ ->
            navController.navigate(
                SpojeniScreenDestination()
            )
            zavrit()
        }
    ),*/
    Oblibene(
        jmeno = R.string.ano,
        icon = Icons.Default.Star,
        multiselect = true,
        onClick = { navController, zavrit, _ ->

            navController.navigate(
                OblibeneScreenDestination()
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
        R.string.typ_dne,
        Icons.Default.Today,
        false,
        onClick = { _, _, _ -> }
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

            val mp = MediaPlayer.create(activity, R.raw.koncime)
            mp.start()

            val audioManager = activity.getSystemService(AudioManager::class.java)

            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)

            mp.setOnCompletionListener {

                activity.finish()
                zavrit()
            }
        }
    )
}