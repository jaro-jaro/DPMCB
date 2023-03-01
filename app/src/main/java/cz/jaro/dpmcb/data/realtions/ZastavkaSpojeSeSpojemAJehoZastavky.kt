package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.helperclasses.Cas

data class ZastavkaSpojeSeSpojemAJehoZastavky(
    val nazev: String,
    val cas: Cas,
    val indexZastavkyNaLince: Int,
    val spojId: String,
    val linka: Int,
    val nizkopodlaznost: Boolean,
    val zastavkySpoje: List<Pair<String, Cas>>,
)
