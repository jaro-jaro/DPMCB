package cz.jaro.dpmcb.data.realtions

import cz.jaro.dpmcb.data.helperclasses.Cas

data class ZastavkaSpojeSeSpojemAJehoZastavkou(
    val nazev: String,
    val cas: Cas,
    val indexZastavkyNaLince: Int,
    val cisloSpoje: Int,
    val linka: Int,
    val nizkopodlaznost: Boolean,
    val jinaZastavkaSpojeNazev: String,
    val jinaZastavkaSpojeCas: Cas,
    val pevneKody: String,
)