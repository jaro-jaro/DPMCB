package cz.jaro.dpmcb.ui.jedouci

sealed interface PraveJedouciState {
    data object Offline : PraveJedouciState

    data object NacitaniLinek : PraveJedouciState

    data object ZadneLinky : PraveJedouciState

    sealed interface LinkyNacteny : PraveJedouciState {
        val cislaLinek: List<Int>
        val filtry: List<Int>
    }

    data class NeniNicVybrano(
        override val cislaLinek: List<Int>,
    ) : LinkyNacteny {
        override val filtry: List<Int> = emptyList()
    }

    data class Nacitani(
        override val cislaLinek: List<Int>,
        override val filtry: List<Int>,
    ) : LinkyNacteny

    data class PraveNicNejede(
        override val cislaLinek: List<Int>,
        override val filtry: List<Int>,
    ) : LinkyNacteny

    data class OK(
        override val cislaLinek: List<Int>,
        override val filtry: List<Int>,
        val seznam: List<JedouciLinkaVeSmeru>,
    ) : LinkyNacteny
}