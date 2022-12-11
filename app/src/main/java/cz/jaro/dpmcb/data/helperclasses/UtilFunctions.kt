package cz.jaro.dpmcb.data.helperclasses

import android.util.Log
import cz.jaro.dpmcb.data.App.Companion.repo
import cz.jaro.dpmcb.data.GraphZastavek
import cz.jaro.dpmcb.data.MutableGraphZastavek
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
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

    fun Int.toSign() = when (sign) {
        -1 -> "-"
        1 -> "+"
        else -> ""
    }
}
