package cz.jaro.dpmcb.ui.odjezdy

import cz.jaro.dpmcb.data.helperclasses.Vysledek
import cz.jaro.dpmcb.ui.vybirator.TypVybiratoru
import java.time.LocalTime

sealed interface OdjezdyEvent {
    data class KliklNaSpoj(val spoj: KartickaState) : OdjezdyEvent
    data class KliklNaZjr(val spoj: KartickaState) : OdjezdyEvent
    data class ZmenitCas(val cas: LocalTime) : OdjezdyEvent
    data class Scrolluje(val i: Int) : OdjezdyEvent
    data class Vybral(val vysledek: Vysledek) : OdjezdyEvent
    data class Zrusil(val typVybiratoru: TypVybiratoru) : OdjezdyEvent
    data object ZmenilKompaktniRezim : OdjezdyEvent
    data object ZmenilJenOdjezdy : OdjezdyEvent
    data object Scrollovat : OdjezdyEvent
    data object DalsiDen : OdjezdyEvent
    data object PredchoziDen : OdjezdyEvent
}
