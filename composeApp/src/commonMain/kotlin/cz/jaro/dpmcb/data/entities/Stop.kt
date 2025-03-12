package cz.jaro.dpmcb.data.entities

import cz.jaro.dpmcb.data.database.Entity

@Entity("", [], false, primaryKeys = ["tab", "stopNumber"], [], [])
data class Stop(
// Primary keys
    val tab: Table,
    val stopNumber: StopNumber,
// Other
    val line: LongLine,
    val stopName: String,
    val fixedCodes: String,
)