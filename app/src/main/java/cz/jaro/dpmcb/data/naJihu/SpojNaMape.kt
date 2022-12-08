package cz.jaro.dpmcb.data.naJihu

/**
 * Reprezentuje 1 spoj, co aktuálně jede
 *
 * @param angle Jakým směrem se na mapě dívá
 * @param delay Kolik minut má zpoždění
 * @param dep Výchozí stanice/zastávka
 * @param depTime Čas odjezdu
 * @param desc Popis linky
 * @param dest Cílová stanice
 * @param destTime Čas příjezdu
 * @param id Id spoje – Shodné s [DetailSpoje.id]
 * @param isDetour Má výluku – Pokud ano, [DetailSpoje.detour] > 0
 * @param lat zem. šířka
 * @param lineNumber číslo linky – Shodné s [DetailSpoje.lineNumber]
 * @param lineType ? – Shodné s [DetailSpoje.lineType] a [ServiceType.lineType]
 * @param lon zem. délka
 * @param name Jméno spoje (Číslo linky + číslo spoje) – Shodné s [DetailSpoje.name]
 * @param operator Dopravce – Shodné s [DetailSpoje.operator]
 * @param serviceNumber Číslo spoje – Shodné s [DetailSpoje.serviceNumber]
 * @param sourceType ?
 * @param time Čas poslední aktualizace
 * @param trainNumber Číslo vlaku
 * @param vehicleType ATV – Shodné s [DetailSpoje.vehicleType] a [ServiceType.vehicleType]
 */
data class SpojNaMape(
    val angle: Int,
    val delay: Int,
    val dep: String,
    val depTime: String,
    val desc: String?,
    val dest: String,
    val destTime: String,
    val id: String,
    val isDetour: Boolean,
    val lat: Double,
    val lineNumber: Int?,
    val lineType: String?,
    val lon: Double,
    val name: String,
    val operator: String,
    val serviceNumber: Int?,
    val sourceType: String,
    val time: String,
    val trainNumber: String?,
    val vehicleType: String,
)
