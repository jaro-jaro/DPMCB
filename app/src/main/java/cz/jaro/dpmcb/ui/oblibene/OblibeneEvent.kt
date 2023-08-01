package cz.jaro.dpmcb.ui.oblibene

import java.time.LocalDate

sealed interface OblibeneEvent {
    data class VybralSpojDnes(val id: String) : OblibeneEvent
    data class VybralSpojJindy(val id: String, val dalsiPojede: LocalDate?) : OblibeneEvent
}