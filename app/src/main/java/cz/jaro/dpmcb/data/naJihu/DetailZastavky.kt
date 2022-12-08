package cz.jaro.dpmcb.data.naJihu

/**
 * Reprezentuje 1 zastávku na mapě
 *
 * @property id Id zastávky
 * @property lat zem. šířka
 * @property lon zem. délka
 * @property name Jméno zastávky
 * @property number Číslo zastávky
 * @property serviceTypes Seznam [ServiceType] co sem jezdí
 * @property sourceType ?
 */
data class DetailZastavky(
    val id: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val number: Int,
    val serviceTypes: List<ServiceType>,
    val sourceType: String,
)
