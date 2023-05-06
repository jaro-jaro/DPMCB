package cz.jaro.dpmcb.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import cz.jaro.dpmcb.data.database.AppDatabase.Companion.Converters
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Smer
import java.time.LocalDate
import java.time.LocalTime

@Database(entities = [CasKod::class, Linka::class, Spoj::class, Zastavka::class, ZastavkaSpoje::class], version = 18)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): Dao

    companion object {
        class Converters {

            @TypeConverter
            fun toSmer(value: Int) = Smer.values()[value]

            @TypeConverter
            fun fromSmer(value: Smer) = value.ordinal

            @TypeConverter
            fun toLocalDate(value: Long) = LocalDate.ofEpochDay(value)

            @TypeConverter
            fun fromLocalDate(value: LocalDate) = value.toEpochDay()

            @TypeConverter
            fun toLocalTime(value: Long?) = value?.let { LocalTime.ofSecondOfDay(it) }

            @TypeConverter
            fun fromLocalTime(value: LocalTime?) = value?.toSecondOfDay()?.toLong()
        }
    }
}
