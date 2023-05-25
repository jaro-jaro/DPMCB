package cz.jaro.dpmcb.data.naJihu

import kotlinx.serialization.Serializable

/**
 * Reprezentuje 1 spoj, co někdy jede
 *
 * @param detour Počet výluk
 * @param distance Délka trasy
 * @param durationHours Doba jízdy v hodinách (zaokrouhleno dolů)
 * @param durationMinutes Doba jízdy v minutách – Pokud je delší než jedna hodina, pouze počet minut poslední necelé hodiny
 * @param fixedCodes Seznam [FixedCode] ?
 * @param geometry Trasa linky ve traru "LINESTRING(l.at l.on, l.at l.on)"
 * @param id Id spoje – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.id]
 * @param isDirectServiceOrigin ?
 * @param isDirectServiceSubseq ?
 * @param isServiceVisibleOnMap Je na mapě? Pokud ano, má i svůj [SpojNaMape] se stejným [id]
 * @param isWalking ?
 * @param lineNumber Číslo linky – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.lineNumber]
 * @param lineType ? – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.lineType]
 * @param name Jméno spoje (Číslo linky + číslo spoje) – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.name]
 * @param operator Dopravce – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.operator]
 * @param serviceNumber Číslo spoje – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.serviceNumber]
 * @param stations Seznam [ZastavkaSpojeNaJihu]
 * @param vehicleType ATV – Pokud [isServiceVisibleOnMap], shodné s [SpojNaMape.vehicleType]
 */
@Serializable
data class DetailSpoje(
    val detour: Int,
    val distance: Int,
    val durationHours: Int,
    val durationMinutes: Int,
    val fixedCodes: List<FixedCode>? = null,
    val geometry: String? = null,
    val id: String,
    val isDirectServiceOrigin: Boolean,
    val isDirectServiceSubseq: Boolean,
    val isServiceVisibleOnMap: Boolean,
    val isWalking: Boolean,
    val lineNumber: Int? = null,
    val lineType: String? = null,
    val name: String,
    val operator: String,
    val serviceNumber: Int? = null,
    val stations: List<ZastavkaSpojeNaJihu>,
    val vehicleType: String,
    val realneZpozdeni: Float? = null,
)
