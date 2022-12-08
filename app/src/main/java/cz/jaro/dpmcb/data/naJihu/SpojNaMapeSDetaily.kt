package cz.jaro.dpmcb.data.naJihu

/**
 *
 * @param angle Jakým směrem se na mapě dívá
 * @param delay Kolik minut má zpoždění
 * @param dep Výchozí stanice/zastávka
 * @param depTime Čas odjezdu
 * @param desc Popis linky
 * @param dest Cílová stanice
 * @param destTime Čas příjezdu
 * @param id Id spoje
 * @param isDetour Má výluku – Pokud ano, [detour] > 0
 * @param lat zem. šířka
 * @param lineNumber číslo linky
 * @param lineType ?
 * @param lon zem. délka
 * @param name Jméno spoje (Číslo linky + číslo spoje)
 * @param operator Dopravce
 * @param serviceNumber Číslo spoje
 * @param sourceType ?
 * @param time Čas poslední aktualizace
 * @param trainNumber Číslo vlaku
 * @param vehicleType ATV – Shodné s [ServiceType.vehicleType]
 * @param detour Počet výluk
 * @param distance Délka trasy
 * @param durationHours Doba jízdy v hodinách (zaokrouhleno dolů)
 * @param durationMinutes Doba jízdy v minutách – Pokud je delší než jedna hodina, pouze počet minut poslední necelé hodiny
 * @param fixedCodes Seznam [FixedCode] ?
 * @param geometry Trasa linky ve traru "LINESTRING(l.at l.on, l.at l.on)"
 * @param isDirectServiceOrigin ?
 * @param isDirectServiceSubseq ?
 * @param isWalking ?
 * @param stations Seznam [ZastavkaSpoje]
 */
data class SpojNaMapeSDetaily(
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
    val detour: Int,
    val distance: Int,
    val durationHours: Int,
    val durationMinutes: Int,
    val fixedCodes: List<FixedCode>?,
    val geometry: String?,
    val isDirectServiceOrigin: Boolean,
    val isDirectServiceSubseq: Boolean,
    val isWalking: Boolean,
    val stations: List<ZastavkaSpoje>,
) {
    companion object {
        operator fun invoke(spojNaMape: SpojNaMape, detailSpoje: DetailSpoje) =
            with(spojNaMape) {
                with(detailSpoje) {
                    SpojNaMapeSDetaily(
                        angle = angle,
                        delay = delay,
                        dep = dep,
                        depTime = depTime,
                        desc = desc,
                        dest = dest,
                        destTime = destTime,
                        id = id,
                        isDetour = isDetour,
                        lat = lat,
                        lineNumber = lineNumber,
                        lineType = lineType,
                        lon = lon,
                        name = name,
                        operator = operator,
                        serviceNumber = serviceNumber,
                        sourceType = sourceType,
                        time = time,
                        trainNumber = trainNumber,
                        vehicleType = vehicleType,
                        detour = detour,
                        distance = distance,
                        durationHours = durationHours,
                        durationMinutes = durationMinutes,
                        fixedCodes = fixedCodes,
                        geometry = geometry,
                        isDirectServiceOrigin = isDirectServiceOrigin,
                        isDirectServiceSubseq = isDirectServiceSubseq,
                        isWalking = isWalking,
                        stations = stations
                    )
                }
            }
    }
}
