package androidx.room

expect annotation class Entity(
    val tableName: String = "",
    val indices: Array<RoomIndex> = [],
    val inheritSuperIndices: Boolean = false,
    val primaryKeys: Array<String> = [],
    val foreignKeys: Array<RoomForeignKey> = [],
    val ignoredColumns: Array<String> = [],
)

expect annotation class RoomIndex
expect annotation class RoomForeignKey