package cz.jaro.dpmcb.ui.jedouci

sealed interface VysledekPraveJedoucich <T>{

    val seznam: List<T>

    data class Poloha(
        override val seznam: List<JedouciLinkaVeSmeru>
    ) : VysledekPraveJedoucich<JedouciLinkaVeSmeru>
    data class Zpozdeni(
        override val seznam: List<JedouciZpozdenySpoj>
    ) : VysledekPraveJedoucich<JedouciZpozdenySpoj>
    data class EvC(
        override val seznam: List<JedouciVuz>
    ) : VysledekPraveJedoucich<JedouciVuz>
}
fun List<JedouciLinkaVeSmeru>.toVysledek() = VysledekPraveJedoucich.Poloha(this)
fun List<JedouciZpozdenySpoj>.toVysledek() = VysledekPraveJedoucich.Zpozdeni(this)
fun List<JedouciVuz>.toVysledek() = VysledekPraveJedoucich.EvC(this)