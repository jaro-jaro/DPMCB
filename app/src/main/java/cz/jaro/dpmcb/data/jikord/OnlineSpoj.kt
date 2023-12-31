package cz.jaro.dpmcb.data.jikord

import cz.jaro.dpmcb.data.helperclasses.LocalTimeSeralizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
@SerialName("OnlineSpoj")
data class OnlineSpoj(
    val id: String,
    val zpozdeniMin: Float?,
    val vuz: Int?,
    val nizkopodlaznost: Boolean?,
    @Serializable(with = LocalTimeSeralizer::class) val pristiZastavka: LocalTime?,
) {
    val linka get() = id.split("-")[1].toInt() - 325_000
}

fun SpojNaMape.toOnlineSpoj(): OnlineSpoj {
    val cn = cn.split("|")
    println(cn)
    return OnlineSpoj(
        id = "S-${cn[0]}-${cn[2]}",
        zpozdeniMin = cn[6].toIntOrNull()?.div(60F),
        vuz = cn[11].toIntOrNull()?.minus(17_000),
        nizkopodlaznost = cn[10] == "1",
        pristiZastavka = if (cn[7].isBlank()) null else LocalTime.of(cn[7].toInt() / 60, cn[7].toInt() % 60),
    )
}