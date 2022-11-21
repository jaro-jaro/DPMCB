package cz.jaro.dpmcb.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas

@Dao
interface ZastavkySpojeDao {
    @Query("SELECT * FROM zastavkaspoje")
    suspend fun getAll(): List<ZastavkaSpoje>

    @Query("SELECT * FROM zastavkaspoje WHERE id=:id LIMIT 1")
    suspend fun findById(id: Long): ZastavkaSpoje

    @Query("SELECT * FROM zastavkaspoje WHERE idSpoje=:id ORDER BY indexNaLince ")
    suspend fun findBySpoj(id: Long): List<ZastavkaSpoje>

    @Query("SELECT nazevZastavky FROM zastavkaspoje WHERE idSpoje=:id ORDER BY indexNaLince")
    suspend fun findNazvyBySpoj(id: Long): List<String>

    @Query("SELECT * FROM zastavkaspoje WHERE cisloLinky=:linka ORDER BY indexNaLince")
    suspend fun findByLinka(linka: Int): List<ZastavkaSpoje>

    @Query("SELECT * FROM zastavkaspoje WHERE nazevKurzu=:kurz")
    suspend fun findByKurz(kurz: String): List<ZastavkaSpoje>

    @Query(
        """
            SELECT * FROM zastavkaspoje 
            JOIN spoj ON zastavkaspoje.idSpoje = spoj.id 
            WHERE zastavkaspoje.cisloLinky=:linka 
            AND zastavkaspoje.nazevKurzu LIKE :kurzLike 
            ORDER BY zastavkaspoje.indexNaLince
        """
    )
    suspend fun findByLinkaAndKurzInExactJoinSpoj(linka: Int, kurzLike: String): Map<ZastavkaSpoje, Spoj>

    @Query(
        """
            SELECT * FROM zastavkaspoje 
            JOIN spoj ON zastavkaspoje.idSpoje = spoj.id 
            WHERE zastavkaspoje.nazevKurzu LIKE :kurzLike 
            ORDER BY zastavkaspoje.indexNaLince
        """
    )
    suspend fun findByKurzInExactJoinSpoj(kurzLike: String): Map<ZastavkaSpoje, Spoj>

    @Query(
        """
            SELECT * FROM zastavkaspoje 
            JOIN spoj ON zastavkaspoje.idSpoje = spoj.id 
            WHERE zastavkaspoje.idSpoje = :spojId 
            ORDER BY zastavkaspoje.indexNaLince
        """
    )
    suspend fun findBySpojIdJoinSpoj(spojId: Long): Map<ZastavkaSpoje, Spoj>

    @Query(
        """
            SELECT * from zastavkaspoje
            JOIN ( -- Vyfiltrovaný spoje
                SELECT s.* FROM zastavkaspoje AS zs
                JOIN spoj AS s ON s.id = zs.idSpoje
                WHERE zs.cisloLinky=:linka 
                AND zs.indexNaLince=:index 
                AND zs.cas!=:cas
            ) AS spoj ON spoj.id = zastavkaspoje.idSpoje 
            ORDER BY zastavkaspoje.indexNaLince
        """
    )
    suspend fun findByLinkaAndIndexAndNotCasJoinSpoj(linka: Int, index: Int, cas: Cas): Map<ZastavkaSpoje, Spoj>

    @Query(
        """
            SELECT * from zastavkaspoje
            JOIN ( -- Vyfiltrovaný spoje
                SELECT s.* FROM zastavkaspoje AS zs
                JOIN spoj AS s ON s.id = zs.idSpoje
                WHERE zs.nazevKurzu LIKE :kurzLike
                AND zs.nazevZastavky = :zastavka
            ) AS spoj ON spoj.id = zastavkaspoje.idSpoje 
            ORDER BY zastavkaspoje.indexNaLince
        """
    )
    suspend fun findByKurzInExactAndIsJoinSpoj(kurzLike: String, zastavka: String): Map<ZastavkaSpoje, Spoj>

    @Insert
    suspend fun insertAll(vararg zastavkySpoje: ZastavkaSpoje)

}
