package cz.jaro.dpmcb.ui.jedouci

sealed interface PraveJedouciState {

    data object NeniDneska : PraveJedouciState

    data object Offline : PraveJedouciState

    data class NacitaniLinek(
        override val typ: TypPraveJedoucich,
    ) : MaPravoMitTyp

    data object ZadneLinky : PraveJedouciState

    sealed interface MaPravoMitFiltry : MaPravoMitTyp {
        val cislaLinek: List<Int>
        val filtry: List<Int>
    }

    sealed interface MaPravoMitTyp : PraveJedouciState {
        val typ: TypPraveJedoucich
    }

//    data class NeniNicVybrano(
//        override val cislaLinek: List<Int>,
//    ) : LinkyNacteny {
//        override val filtry: List<Int> = emptyList()
//    }

    data class Nacitani(
        override val cislaLinek: List<Int>,
        override val filtry: List<Int>,
        override val typ: TypPraveJedoucich,
    ) : MaPravoMitFiltry, MaPravoMitTyp

    data class PraveNicNejede(
        override val cislaLinek: List<Int>,
        override val filtry: List<Int>,
        override val typ: TypPraveJedoucich,
    ) : MaPravoMitFiltry, MaPravoMitTyp

    data class OK(
        override val cislaLinek: List<Int>,
        override val filtry: List<Int>,
        override val typ: TypPraveJedoucich,
        val vysledek: VysledekPraveJedoucich<*>,
    ) : MaPravoMitFiltry, MaPravoMitTyp
}