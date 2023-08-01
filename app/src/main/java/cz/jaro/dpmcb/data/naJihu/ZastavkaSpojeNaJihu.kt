package cz.jaro.dpmcb.data.naJihu

import kotlinx.serialization.Serializable

/**
 * Reprezentuje 1 zastávku, na které daný spoj staví
 *
 * @property arrivalTime Čas příjezdu na zastávku
 * @property departureTime Čas odjezdu ze zastávky
 * @property fixedCodes Seznam [FixedCode] ?
 * @property id Id Zastávky – Shodné s [DetailZastavky.id]
 * @property name Jméno zastávky – Shodné s [DetailZastavky.name]
 * @property passed Indikuje, jestli spoj již na zastávce zastavil
 */
@Serializable
data class ZastavkaSpojeNaJihu(
    val arrivalTime: String? = null,
    val departureTime: String? = null,
    val fixedCodes: List<FixedCode>? = null,
    val id: String,
    val name: String,
    val passed: Boolean,
)
