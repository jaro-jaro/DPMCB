package cz.jaro.dpmcb.data.naJihu

/**
 * Reprezentuje spoj, který v nejbližší době odjede od dané zastávky
 *
 * @property dest Cílová stanice – Shodná s [ZastavkaSpojeNaJihu.name] poslední ze stanic [DetailSpoje.stations]
 * @property id Id spoje – Shodné s [DetailSpoje.id]
 * @property lineType ? – Shodné s [DetailSpoje.lineType]
 * @property name Jméno spoje (Číslo linky + číslo spoje) – Shodné s [DetailSpoje.name]
 * @property sourceType ? – Shodné s [DetailSpoje.lineType]
 * @property time Čas odjezdu z této zastávky – Shodné s [ZastavkaSpojeNaJihu.departureTime] u stanice z [DetailSpoje.stations] s odpovídajícím [ZastavkaSpojeNaJihu.id]
 * @property vehicleType ATV – Shodné s [DetailSpoje.vehicleType]
 */
data class OdjezdSpoje(
    val dest: String,
    val id: String,
    val lineType: String,
    val name: String,
    val sourceType: String,
    val time: String,
    val vehicleType: String,
)
