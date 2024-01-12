package cz.jaro.dpmcb.ui.jedouci

enum class TypPraveJedoucich {
    Poloha {
        override val jmeno get() = "linky"
    }, Zpozdeni {
        override val jmeno get() = "zpoždění"
    }, EvC {
        override val jmeno get() = "čísla vozu"
    };

    abstract val jmeno: String
}
