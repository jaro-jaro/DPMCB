package cz.jaro.dpmcb.ui.jedouci

enum class TypPraveJedoucich {
    Poloha {
        override val jmeno get() = "polohy"
    }, Zpozdeni {
        override val jmeno get() = "zpoždění"
    }, EvC {
        override val jmeno get() = "vozu"
    };

    abstract val jmeno: String
}
