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
import java.time.LocalDate
import java.time.LocalTime

@Database(
    entities = [TimeCode::class, Line::class, Conn::class, Stop::class, ConnStop::class],
    version = 27,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): Dao

    companion object {
        class Converters {

            @TypeConverter
            fun toSmer(value: Int) = Direction.entries[value]

            @TypeConverter
            fun fromSmer(value: Direction) = value.ordinal

            @TypeConverter
            fun toLocalDate(value: Long) = LocalDate.ofEpochDay(value)!!

            @TypeConverter
            fun fromLocalDate(value: LocalDate) = value.toEpochDay()

            @TypeConverter
            fun toLocalTime(value: Long?) = value?.let { LocalTime.ofSecondOfDay(it) }

            @TypeConverter
            fun fromLocalTime(value: LocalTime?) = value?.toSecondOfDay()?.toLong()
        }
    }
}
