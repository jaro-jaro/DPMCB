package cz.jaro.dpmcb.data.realtions

import java.time.LocalTime

data class ZastavkaSpojeSeSpojemAJehoZastavky(
    val nazev: String,
    val cas: LocalTime,
    val indexZastavkyNaLince: Int,
    val spojId: String,
    val linka: Int,
    val nizkopodlaznost: Boolean,
    val zastavkySpoje: List<NazevCasIndexNaLince>,
)
