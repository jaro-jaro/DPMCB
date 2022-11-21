package cz.jaro.dpmcb.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cz.jaro.dpmcb.data.entities.Spoj

@Dao
interface SpojeDao {
    @Query("SELECT * FROM spoj")
    suspend fun getAll(): List<Spoj>

    @Query("SELECT * FROM spoj WHERE id=:id LIMIT 1")
    suspend fun findById(id: Long): Spoj

    @Query("SELECT * FROM spoj WHERE cisloLinky=:linka")
    suspend fun findByLinka(linka: Int): List<Spoj>

    @Query("SELECT * FROM spoj WHERE nazevKurzu=:kurz")
    suspend fun findByKurz(kurz: String): List<Spoj>

    @Query("SELECT * FROM spoj WHERE nazevKurzu LIKE :kurzLike")
    suspend fun findByKurzInExact(kurzLike: String): List<Spoj>

    @Insert
    suspend fun insertAll(vararg spoje: Spoj)
}
