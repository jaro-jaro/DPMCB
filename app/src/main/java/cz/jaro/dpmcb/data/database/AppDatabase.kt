package cz.jaro.dpmcb.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import cz.jaro.datum_cas.Cas
import cz.jaro.datum_cas.Datum
import cz.jaro.datum_cas.dni
import cz.jaro.datum_cas.sek
import cz.jaro.datum_cas.toCas
import cz.jaro.datum_cas.toDatum
import cz.jaro.dpmcb.data.database.AppDatabase.Companion.Converters
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Smer

@Database(entities = [CasKod::class, Linka::class, Spoj::class, Zastavka::class, ZastavkaSpoje::class], version = 12)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): Dao

    companion object {
        class Converters {
            @TypeConverter
            fun toCas(value: Int) = value.sek.toCas()

            @TypeConverter
            fun fromCas(value: Cas) = value.toTrvani().sek

            @TypeConverter
            fun toDatum(value: Int) = value.dni.toDatum()

            @TypeConverter
            fun fromDatum(value: Datum) = value.toTrvani().dni

            @TypeConverter
            fun toSmer(value: Int) = Smer.values()[value]

            @TypeConverter
            fun fromSmer(value: Smer) = value.ordinal
        }
    }
}
