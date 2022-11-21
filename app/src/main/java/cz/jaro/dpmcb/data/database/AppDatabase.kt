package cz.jaro.dpmcb.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import cz.jaro.dpmcb.data.database.AppDatabase.Companion.CasConverter
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas

@Database(entities = [Spoj::class, ZastavkaSpoje::class], version = 1)
@TypeConverters(CasConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spojeDao(): SpojeDao
    abstract fun zastavkySpojeDao(): ZastavkySpojeDao

    companion object {
        class CasConverter {
            @TypeConverter
            fun toCas(value: Int) = value.toCas()

            @TypeConverter
            fun fromCas(value: Cas) = value.toInt()
        }
    }
}
