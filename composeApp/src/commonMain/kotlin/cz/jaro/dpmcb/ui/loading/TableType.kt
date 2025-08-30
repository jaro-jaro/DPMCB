package cz.jaro.dpmcb.ui.loading

import kotlinx.serialization.Serializable

@Serializable
enum class TableType {
    Pevnykod,
    Zaslinky,
    Zasspoje,
    Zastavky,
    VerzeJDF,
    Dopravci,
    Caskody,
    LinExt,
    Linky,
    Spoje,
    Udaje,
}