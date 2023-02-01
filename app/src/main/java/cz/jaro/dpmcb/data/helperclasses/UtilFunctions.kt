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
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.GraphZastavek
import cz.jaro.dpmcb.data.MutableGraphZastavek
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.sign

object UtilFunctions {

    fun MutableGraphZastavek.toGraphZastavek(): GraphZastavek = map { (k, set) -> k to set.toSet() }.toMap()
    fun GraphZastavek.toMutableGraphZastavek(): MutableGraphZastavek = map { (k, set) -> k to set.toMutableSet() }.toMap().toMutableMap()

    fun emptyGraphZastavek(): GraphZastavek = mapOf()

    enum class VDP { DNY, VIKENDY, PRAZDNINY }

    fun VDP.toChar() = when (this) {
        VDP.VIKENDY -> 'V'
        VDP.DNY -> 'D'
        VDP.PRAZDNINY -> 'P'
    }

    fun Char.toVDP() = when (this) {
        'V' -> VDP.VIKENDY
        'D' -> VDP.DNY
        'P' -> VDP.PRAZDNINY
        else -> throw IllegalArgumentException("$this není V ani D ani P!")
    }

    fun Smer.toInt(): Int = when (this) {
        Smer.POZITIVNI -> 1
        Smer.NEGATIVNI -> -1
    }

    inline fun <T> List<T>.reversedIf(predicate: (List<T>) -> Boolean): List<T> = if (predicate(this)) this.reversed() else this

    suspend inline fun <R> List<String>.proVsechnyIndexy(zastavka: String, crossinline operation: suspend (index: Int) -> R): List<R> = this
        .vsechnyIndexy(zastavka)
        .map { index -> operation(index) }

    fun List<String>.vsechnyIndexy(zastavka: String): List<Int> = this
        .asSequence()
        .mapIndexed { i, z -> i to z }
        .filter { (_, z) -> z == zastavka }
        .map { (index, _) -> index }
        .toList()

    @JvmName("vsechnyIndexyZastavkaSpoje")
    fun List<ZastavkaSpoje>.vsechnyIndexy(zastavka: String): List<Int> = map { it.nazevZastavky }.vsechnyIndexy(zastavka)

    suspend fun Spoj.zastavkySpoje() = repo.zastavkySpoje(id)
    suspend fun Spoj.nazvyZastavek() = repo.nazvyZastavekSpoje(id)

    suspend fun Spoj.pristiZastavka(
        indexTyhleZastavky: Int,
    ): ZastavkaSpoje? {
        val zastavek = zastavkySpoje().size
        return when (smer) {
            Smer.POZITIVNI -> zastavkySpoje().toList().subList(indexTyhleZastavky + 1, zastavek)
            Smer.NEGATIVNI -> zastavkySpoje().toList().subList(0, indexTyhleZastavky).reversed()
        }.find { zastavka -> zastavka.cas != Cas.nikdy }
    }

    fun List<ZastavkaSpoje>.pristiZastavka(
        smerSpoje: Smer,
        indexTyhleZastavky: Int,
    ): ZastavkaSpoje? {
        val zastavek = size
        require(indexTyhleZastavky < zastavek) { "indexTyhleZastavky($indexTyhleZastavky) nesmí být vyšší nebo roven počtu zastávek($zastavek)" }
        return when (smerSpoje) {
            Smer.POZITIVNI -> subList(indexTyhleZastavky + 1, zastavek)
            Smer.NEGATIVNI -> subList(0, indexTyhleZastavky).reversed()
        }.find { zastavka -> zastavka.cas != Cas.nikdy }
    }


    val Datum.typDne: VDP
        get() = toCalendar()[Calendar.DAY_OF_WEEK].let { denVTydnu ->
            if (denVTydnu in listOf(1, 7) || this in listOf<Datum>( /* Svátky */)) VDP.VIKENDY
            else if (this in listOf<Datum>( /* Prázdniny */)) VDP.PRAZDNINY
            else VDP.DNY
        }

    fun <R> funguj(vararg msg: R?): Unit = run { Log.d("funguj", msg.joinToString()) }
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
        tint: Color = LocalContentColor.current,
    ) {
        if (contentDescription != null) PlainTooltipBox(tooltip = { Text(text = contentDescription) }) {
            Icon(imageVector, contentDescription, modifier, tint)
        }
        else Icon(imageVector, null, modifier, tint)
    }

    inline fun <reified T, R> Iterable<Flow<T>>.combine(crossinline transform: suspend (List<T>) -> R) =
        kotlinx.coroutines.flow.combine(this) { transform(it.toList()) }

}
