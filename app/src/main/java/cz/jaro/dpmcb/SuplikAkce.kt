package cz.jaro.dpmcb

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ramcosta.composedestinations.navigation.navigate
import cz.jaro.dpmcb.data.helperclasses.TypAdapteru
import cz.jaro.dpmcb.ui.destinations.MapaScreenDestination
import cz.jaro.dpmcb.ui.destinations.OblibeneScreenDestination
import cz.jaro.dpmcb.ui.destinations.PraveJedouciScreenDestination
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
        jmeno = R.string.oblibena,
        icon = Icons.Default.Star,
        multiselect = true,
        onClick = { navController, zavrit, _ ->

            navController.navigate(
                OblibeneScreenDestination()
            )
            zavrit()
        }
    ),
    Odjezdy(
        jmeno = R.string.odjezdy,
        icon = Icons.Default.DepartureBoard,
        multiselect = true,
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
    PraveJedouci(
        jmeno = R.string.prave_jedouci,
        icon = Icons.Default.FastForward,
        multiselect = true,
        onClick = { navController, zavrit, _ ->

            navController.navigate(
                PraveJedouciScreenDestination()
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
    Prukazka(
        jmeno = R.string.prukazka,
        icon = Icons.Rounded.QrCode2,
        multiselect = true,
        onClick = { navController, zavrit, ctx ->

//            navController.navigate(
//                MapaScreenDestination()
//            )
//            zavrit()
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Již brzy")
                .setMessage(
                    "někdy příště (můžete se podívat na náš GH #22\n" + StringBuilder().apply {
                        repeat(16) {
                            repeat(16) {
                                val r = kotlin.random.Random.Default.nextInt(1, 1024)
                                append(
                                    when {
                                        r > 912 -> "Σ"
                                        r > 845 -> "ℂ"
                                        r > 764 -> "∅"
                                        r > 712 -> "∈"
                                        r > 700 -> "\uD83C\uDF51"
                                        r > 599 -> "⇎"
                                        r > 512 -> "e"
                                        r > 445 -> "⇒"
                                        r > 318 -> "π"
                                        r > 314 -> "\uD835\uDF45"
                                        r > 256 -> "\uD83E\uDDF9"
                                        r > 255 -> "\uD83D\uDC08"
                                        r > 100 -> "-"
                                        r > 80 -> ","
                                        else -> "."
                                    }
                                )
                            }
                            append("\n")
                        }
                    }.toString()
                )
                .show()
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