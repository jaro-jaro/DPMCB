package cz.jaro.dpmcb.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import cz.jaro.dpmcb.data.database.AppDatabase.Companion.Converters
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.types.Direction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Database(
    entities = [TimeCode::class, Line::class, Conn::class, Stop::class, ConnStop::class],
    version = 28,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): Dao

    companion object {
        class Converters {
            @TypeConverter
            fun toDirection(value: Int) = Direction.entries[value]
            @TypeConverter
            fun fromDirection(value: Direction) = value.ordinal

            @TypeConverter
            fun toLocalDate(value: String) = LocalDate.parse(value)
            @TypeConverter
            fun fromLocalDate(value: LocalDate) = value.toString()

            @TypeConverter
            fun toLocalTime(value: String?) = value?.let { LocalTime.parse(it) }
            @TypeConverter
            fun fromLocalTime(value: LocalTime?) = value?.toString()
        }
    }
}
