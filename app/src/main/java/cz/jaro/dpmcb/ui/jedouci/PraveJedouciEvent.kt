package cz.jaro.dpmcb.ui.jedouci

sealed interface PraveJedouciEvent {
    data class ZmenitTyp(val typ: TypPraveJedoucich) : PraveJedouciEvent
    data class ZmenitFiltr(val cisloLinky: Int) : PraveJedouciEvent
    data class KliklNaSpoj(val spojId: String) : PraveJedouciEvent
    data class KliklNaKurz(val kurz: String) : PraveJedouciEvent
}