package cz.jaro.dpmcb.ui.now_running

enum class NowRunningType {
    Line {
        override val label get() = "linky"
    }, Delay {
        override val label get() = "zpoždění"
    }, RegN {
        override val label get() = "čísla vozu"
    };

    abstract val label: String
}
