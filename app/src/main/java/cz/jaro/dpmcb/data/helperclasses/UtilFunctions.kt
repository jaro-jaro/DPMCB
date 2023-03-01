package cz.jaro.dpmcb.data.helperclasses

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cz.jaro.dpmcb.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlin.math.sign

object UtilFunctions {

    fun Smer.toInt(): Int = when (this) {
        Smer.POZITIVNI -> 1
        Smer.NEGATIVNI -> -1
    }

    fun <T> ifTake(condition: Boolean, take: () -> T): T? = if (condition) take() else null

    inline fun <T> List<T>.reversedIf(predicate: (List<T>) -> Boolean): List<T> = if (predicate(this)) this.reversed() else this

//    suspend inline fun <R> List<String>.proVsechnyIndexy(zastavka: String, crossinline operation: suspend (index: Int) -> R): List<R> = this
//        .vsechnyIndexy(zastavka)
//        .map { index -> operation(index) }

//    suspend inline fun <R> List<Dao.NazevZastavkySIndexem>.proVsechnyIndexy(zastavka: String, crossinline operation: suspend (index: Int) -> R): List<R> = this
//        .vsechnyIndexy(zastavka)
//        .map { index -> operation(index) }
//
//    fun List<Dao.NazevZastavkySIndexem>.vsechnyIndexy(zastavka: String): List<Int> = this
//        .asSequence()
//        .filter { (z, _) -> z == zastavka }
//        .map { (_, index) -> index }
//        .toList()

//    @JvmName("vsechnyIndexyZastavkaSpoje")
//    fun List<ZastavkaSpoje>.vsechnyIndexy(zastavka: String): List<Int> = map { it.nazevZastavky }.vsechnyIndexy(zastavka)

//    suspend fun Spoj.zastavkySpoje() = repo.zastavkySpoje(id)
//    suspend fun Spoj.nazvyZastavek() = repo.nazvyZastavekSpoje(id)

//    suspend fun Spoj.pristiZastavka(
//        indexTyhleZastavky: Int,
//    ): ZastavkaSpoje? {
//        val zastavek = zastavkySpoje().size
//        return when (smer) {
//            Smer.POZITIVNI -> zastavkySpoje().toList().subList(indexTyhleZastavky + 1, zastavek)
//            Smer.NEGATIVNI -> zastavkySpoje().toList().subList(0, indexTyhleZastavky).reversed()
//        }.find { zastavka -> zastavka.cas != Cas.nikdy }
//    }

//    fun List<ZastavkaSpoje>.pristiZastavka(
//        smerSpoje: Smer,
//        indexTyhleZastavky: Int,
//    ): ZastavkaSpoje? {
//        return when (smerSpoje) {
//            Smer.POZITIVNI -> filter { it.indexZastavkyNaLince > indexTyhleZastavky }.find { zastavka -> zastavka.cas != Cas.nikdy }
//            Smer.NEGATIVNI -> subList(0, indexTyhleZastavky).reversed().find { zastavka -> zastavka.cas != Cas.nikdy }
//        }
//    }

    fun <R> funguj(vararg msg: R?) = run { if (BuildConfig.DEBUG) Log.d("funguj", msg.joinToString()) }
    inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.funguj(vararg msg: R, transform: T.() -> S): T =
        also { UtilFunctions.funguj(this.transform(), *msg) }

    inline fun <reified T : Any?, reified R : Any?> T.funguj(vararg msg: R): T = also { funguj(*msg, transform = { this }) }
    inline fun <reified T : Any?, reified S : Any?> T.funguj(transform: T.() -> S = { this as S }): T =
        also { funguj(*emptyArray<Any?>(), transform = transform) }

    inline fun <reified T : Any?> T.funguj(): T = also { funguj(*emptyArray<Any?>(), transform = { this }) }

    fun Int.toSign() = when (sign) {
        -1 -> "-"
        1 -> "+"
        else -> ""
    }

    @Composable
    fun barvaZpozdeniTextu(zpozdeni: Int) = when {
        zpozdeni > 5 -> Color.Red
        zpozdeni > 0 -> Color(0xFFCC6600)
        else -> Color.Green
    }

    @Composable
    fun barvaZpozdeniBublinyText(zpozdeni: Int) = when {
        zpozdeni > 5 -> MaterialTheme.colorScheme.onErrorContainer
        zpozdeni > 0 -> Color(0xFFffddaf)
        else -> Color(0xFFADF0D8)
    }

    @Composable
    fun barvaZpozdeniBublinyKontejner(zpozdeni: Int) = when {
        zpozdeni > 5 -> MaterialTheme.colorScheme.errorContainer
        zpozdeni > 0 -> Color(0xFF614000)
        else -> Color(0xFF015140)
    }

    fun Offset(x: Float = 0F, y: Float = 0F) = androidx.compose.ui.geometry.Offset(x, y)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun IconWithTooltip(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tooltipText: String? = contentDescription,
        tint: Color = LocalContentColor.current,
    ) {
        if (tooltipText != null) PlainTooltipBox(tooltip = { Text(text = tooltipText) }) {
            Icon(imageVector, contentDescription, modifier, tint)
        }
        else Icon(imageVector, contentDescription, modifier, tint)
    }

    inline fun <reified T, R> Iterable<Flow<T>>.combine(crossinline transform: suspend (List<T>) -> R) =
        kotlinx.coroutines.flow.combine(this) { transform(it.toList()) }

}
