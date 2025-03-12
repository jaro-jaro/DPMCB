package cz.jaro.dpmcb.data.database

actual annotation class Entity(
    actual val tableName: String,
    actual val indices: Array<RoomIndex>,
    actual val inheritSuperIndices: Boolean,
    actual val primaryKeys: Array<String>,
    actual val foreignKeys: Array<RoomForeignKey>,
    actual val ignoredColumns: Array<String>,
)

actual annotation class RoomIndex()
actual annotation class RoomForeignKey()