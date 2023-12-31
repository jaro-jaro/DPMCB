package cz.jaro.dpmcb.data.jikord


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SpojNaMape")
/**
 * Spoj na mapě
 * @param t "H"/"T" - Autobus/vlak
 * @param c Číslo vlakového dopravce, u autobusů 0
 * @param dm ??
 * @param d 9 ??
 * @param ds Zpoždění/předčas, tvary: "$z?~min.~?~s"/"$p-?~min.~?~s", kde ? je libovolné číslo
 * @param n ??
 * @param cr Celý název vlaku, u autobusu null
 * @param e ??
 * @param x Zeměpisná délka
 * @param y Zeměpisná šířka
 * @param dt Datum (čeho?), tvar: "dd.mm.yyyy"
 * @param tm Čas aktualizace (?), tvar: "hh:mm:ss"
 * @param ico Název ikonky
 * @param cn Informace o spoji - seznam informací oddělený svislítkami ("|"). Autobusy: 0 Celé číslo linky; 1 Zkrácené číslo linky; 2 Číslo spoje; 3 Poslední zastávka; 4 Poslední zpoždění; 5 Příští zastávka; 6 Příští zpoždění (s); 7 Pravidelný odjezd z následující zastávky (min); 8 Výchozí zastávka; 9 Cílová zastávka; 10 Nízkopodlažnost (0/1); 11 Ev. číslo vozu; 12 ??; 13 ??. Vlaky: 0 Celý název vlaku; 1 Linka; 2 Číslo vlaku; 3 Poslední zastávka; 4 Poslední zpoždění (s); 9 Cílová stanice; 10 ??
 * @param plf true ??
 * @param cat 2/1 - Autobus/vlak
 */
data class SpojNaMape(
    @SerialName("t") val t: String,
    @SerialName("c") val c: Int,
    @SerialName("dm") val dm: Int,
    @SerialName("d") val d: Int,
    @SerialName("ds") val ds: String,
    @SerialName("n") val n: String?,
    @SerialName("cr") val cr: String,
    @SerialName("e") val e: Nothing?,
    @SerialName("x") val x: Double,
    @SerialName("y") val y: Double,
    @SerialName("dt") val dt: String,
    @SerialName("tm") val tm: String,
    @SerialName("ico") val ico: String,
    @SerialName("cn") val cn: String,
    @SerialName("plf") val plf: Boolean,
    @SerialName("cat") val cat: Int,
)