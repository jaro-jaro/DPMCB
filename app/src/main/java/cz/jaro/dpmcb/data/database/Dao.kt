package cz.jaro.dpmcb.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Datum
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.realtions.LinkaNizkopodlaznostCasNazevSpojId
import cz.jaro.dpmcb.data.realtions.NazevACas
import cz.jaro.dpmcb.data.realtions.OdjezdNizkopodlaznostSpojId
import cz.jaro.dpmcb.data.realtions.ZastavkaSpojeSeSpojemAJehoZastavkou

@Dao
interface Dao {
    @Query(
        """
        SELECT DISTINCT nazevZastavky FROM zastavka
        ORDER BY nazevZastavky
    """
    )
    suspend fun nazvyZastavek(): List<String>

    @Query(
        """
        SELECT DISTINCT kratkeCislo FROM linka
        ORDER BY kratkeCislo
    """
    )
    suspend fun cislaLinek(): List<Int>

    @Query(
        """
        WITH spojeZdeJedouci AS (
            SELECT DISTINCT spoj.* FROM zastavkaspoje
            JOIN spoj ON spoj.linka = zastavkaspoje.linka AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            WHERE zastavka.nazevZastavky = :tahleZastavka
            AND zastavkaspoje.linka = :linka
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )   
        ),
        indexyTyhleZastavky AS (
            SELECT DISTINCT zastavkaSpoje.indexZastavkyNaLince FROM zastavkaSpoje
             JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
             JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka =  zastavkaspoje.linka
             WHERE zastavkaSpoje.linka = :linka
             AND zastavka.nazevZastavky = :tahleZastavka
             ORDER BY zastavkaSpoje.indexZastavkyNaLince 
        ), 
        negativni(max, nazevZastavky, indexNaLince, linka, cisloSpoje) AS (
            SELECT DISTINCT MAX(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
            CROSS JOIN indexyTyhleZastavky tahleZastavka
            WHERE zastavkaspoje.indexZastavkyNaLince < tahleZastavka.indexZastavkyNaLince
            AND spoj.smer <> :pozitivni
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY -zastavkaSpoje.indexZastavkyNaLince
        ),
        pozitivni(min, nazevZastavky, indexNaLince, linka, cisloSpoje) AS (
            SELECT DISTINCT MIN(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
            CROSS JOIN indexyTyhleZastavky tahleZastavka
            WHERE zastavkaspoje.indexZastavkyNaLince > tahleZastavka.indexZastavkyNaLince
            AND spoj.smer = :pozitivni
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY zastavkaSpoje.indexZastavkyNaLince
        )
        SELECT DISTINCT nazevZastavky
        FROM pozitivni
        UNION
        SELECT DISTINCT nazevZastavky
        FROM negativni
    """
    )
    suspend fun pristiZastavky(linka: Int, tahleZastavka: String, nikdy: Cas = Cas.nikdy, pozitivni: Smer = Smer.POZITIVNI): List<String>

    @Query(
        """
        SELECT DISTINCT zastavka.nazevZastavky FROM zastavkaSpoje
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaSpoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
        JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
        WHERE zastavkaSpoje.linka = :linka
        ORDER BY zastavkaSpoje.indexZastavkyNaLince
    """
    )
    suspend fun nazvyZastavekLinky(linka: Int): List<String>

    @Query(
        """
        WITH pocetCaskoduSpoje AS (
            SELECT DISTINCT spoj.id, COUNT(caskod.indexTerminu) pocet FROM zastavkaspoje
            JOIN spoj ON spoj.linka = zastavkaspoje.linka AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN caskod ON caskod.cisloSpoje = zastavkaspoje.cisloSpoje AND caskod.linka = zastavkaspoje.linka
            WHERE zastavka.nazevZastavky = :zastavka
            AND spoj.linka = :linka
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            GROUP BY spoj.id
        ),
        spojeZdeJedouci AS (
            SELECT DISTINCT spoj.*, COUNT(caskod.indexTerminu) FROM zastavkaspoje
            JOIN spoj ON spoj.linka = zastavkaspoje.linka AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN caskod ON caskod.cisloSpoje = zastavkaspoje.cisloSpoje AND caskod.linka = zastavkaspoje.linka
            JOIN pocetCaskoduSpoje ON pocetCaskoduSpoje.id = spoj.id
            WHERE zastavka.nazevZastavky = :zastavka
            AND spoj.linka = :linka
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            AND ((
                caskod.jede 
                AND caskod.platiOd <= :datum
                AND :datum <= caskod.platiDo
            ) OR (
                NOT caskod.jede 
                AND NOT (
                    caskod.platiOd <= :datum
                    AND :datum <= caskod.platiDo
                )
            ))
            GROUP BY spoj.id
            HAVING (
                caskod.jede 
                AND COUNT(caskod.indexTerminu) >= 1
            ) OR (
                NOT caskod.jede
                AND COUNT(caskod.indexTerminu) = pocetCaskoduSpoje.pocet
            )
        ),
        indexyTyhleZastavky AS (
            SELECT DISTINCT zastavkaSpoje.indexZastavkyNaLince FROM zastavkaSpoje
             JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
             JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka =  zastavkaspoje.linka
             WHERE zastavkaSpoje.linka = :linka
             AND zastavka.nazevZastavky = :zastavka
             ORDER BY zastavkaSpoje.indexZastavkyNaLince
        ),
        negativni(max, nazevZastavky, indexNaLince, linka, cisloSpoje, indexTyhleNaLince) AS (
            SELECT DISTINCT MAX(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
            CROSS JOIN indexyTyhleZastavky tahleZastavka
            WHERE zastavkaspoje.indexZastavkyNaLince < tahleZastavka.indexZastavkyNaLince
            AND spoj.smer <> :pozitivni
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY -zastavkaSpoje.indexZastavkyNaLince
        ),
        pozitivni(min, nazevZastavky, indexNaLince, linka, cisloSpoje, indexTyhleNaLince) AS (
            SELECT DISTINCT MIN(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
            CROSS JOIN indexyTyhleZastavky tahleZastavka
            WHERE zastavkaspoje.indexZastavkyNaLince > tahleZastavka.indexZastavkyNaLince
            AND spoj.smer = :pozitivni
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY zastavkaSpoje.indexZastavkyNaLince
        )
        SELECT DISTINCT zastavkaspoje.odjezd, (spoj.pevnekody LIKE '%24%') nizkopodlaznost, spoj.id spojId, spoj.pevneKody
        FROM zastavkaspoje
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
        JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
        CROSS JOIN indexyTyhleZastavky tahleZastavka ON zastavkaspoje.indexZastavkyNaLince = tahleZastavka.indexZastavkyNaLince
        LEFT JOIN pozitivni ON pozitivni.cisloSpoje = spoj.cisloSpoje AND pozitivni.indexTyhleNaLince = zastavkaspoje.indexZastavkyNaLince
        LEFT JOIN negativni ON negativni.cisloSpoje = spoj.cisloSpoje AND negativni.indexTyhleNaLince = zastavkaspoje.indexZastavkyNaLince
        WHERE (spoj.smer = :pozitivni AND pozitivni.nazevZastavky = :pristiZastavka)
        OR (spoj.smer <> :pozitivni AND negativni.nazevZastavky = :pristiZastavka)
    """
    )
    suspend fun zastavkyJedouciVDatumSPristiZastavkou(
        linka: Int,
        zastavka: String,
        pristiZastavka: String,
        datum: Datum,
        nikdy: Cas = Cas.nikdy,
        pozitivni: Smer = Smer.POZITIVNI,
    ): List<OdjezdNizkopodlaznostSpojId>

    @Transaction
    @Query(
        """
        SELECT (spoj.pevnekody LIKE '%24%') nizkopodlaznost, spoj.linka, spoj.pevneKody, CASE
            WHEN zastavkaspoje.odjezd = NULL OR zastavkaspoje.odjezd = :nikdy THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END cas, nazevZastavky nazev, spoj.id spojId FROM zastavkaspoje
        JOIN spoj ON spoj.linka = zastavkaspoje.linka AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
        JOIN zastavka ON zastavka.linka = zastavkaspoje.linka AND zastavka.cisloZastavky = zastavkaspoje.cisloZastavky 
        WHERE (
            NOT zastavkaspoje.odjezd = :nikdy
            OR NOT zastavkaspoje.prijezd = :nikdy
        )
        AND spoj.id = :spojId
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun spojSeZastavkySpojeNaKterychStavi(spojId: String, nikdy: Cas = Cas.nikdy, pozitivni: Smer = Smer.POZITIVNI): List<LinkaNizkopodlaznostCasNazevSpojId>

    @Query(
        """
        WITH spojeZdeJedouci AS (
            SELECT DISTINCT spoj.* FROM zastavkaspoje
            JOIN spoj ON spoj.linka = zastavkaspoje.linka AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN caskod ON caskod.cisloSpoje = zastavkaspoje.cisloSpoje AND zastavka.linka = zastavkaspoje.linka
            WHERE zastavka.nazevZastavky = :zastavka
            AND (
                NOT zastavkaspoje.odjezd = :nikdy
                OR NOT zastavkaspoje.prijezd = :nikdy
            )
            AND ((
                caskod.jede 
                AND caskod.platiOd <= :datum
                AND :datum <= caskod.platiDo
            ) OR (
                NOT caskod.jede 
                AND NOT (
                    caskod.platiOd <= :datum
                    AND :datum <= caskod.platiDo
                )
            ))
        ),
        indexyTyhleZastavky AS (
            SELECT DISTINCT zastavkaSpoje.indexZastavkyNaLince, zastavkaspoje.linka FROM zastavkaSpoje
             JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
             JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka =  zastavkaspoje.linka
             AND zastavka.nazevZastavky = :zastavka
             ORDER BY zastavkaSpoje.indexZastavkyNaLince
        ),
        tahleZastavka AS (
            SELECT zastavka.nazevZastavky nazev, spoj.pevneKody, CASE
                WHEN zastavkaspoje.odjezd = NULL OR zastavkaspoje.odjezd = :nikdy THEN zastavkaspoje.prijezd
                ELSE zastavkaspoje.odjezd
            END cas, zastavkaspoje.indexZastavkyNaLince, spoj.cisloSpoje, spoj.linka, (spoj.pevnekody LIKE '%24%') nizkopodlaznost FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
            CROSS JOIN indexyTyhleZastavky tahleZastavka ON zastavkaspoje.linka = tahleZastavka.linka AND zastavkaspoje.indexZastavkyNaLince = tahleZastavka.indexZastavkyNaLince
        )
        SELECT tahleZastavka.*, CASE
            WHEN zastavkaspoje.odjezd = NULL OR zastavkaspoje.odjezd = :nikdy THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END jinaZastavkaSpojeCas, zastavka.nazevZastavky jinaZastavkaSpojeNazev FROM tahleZastavka
        JOIN zastavkaspoje ON zastavkaspoje.cisloSpoje = tahleZastavka.cisloSpoje AND zastavkaspoje.linka = tahleZastavka.linka
        JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.linka = zastavkaspoje.linka
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun spojeJedouciVdatumZastavujiciNaIndexechZastavkySeZastavkySpoje(
        datum: Datum, zastavka: String, nikdy: Cas = Cas.nikdy, pozitivni: Smer = Smer.POZITIVNI,
    ): List<ZastavkaSpojeSeSpojemAJehoZastavkou>

    @Transaction
    @Query(
        """
        SELECT spoj.*, CASE
            WHEN zastavkaspoje.odjezd = NULL OR zastavkaspoje.odjezd = :nikdy THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END cas, zastavka.nazevZastavky nazev FROM spoj
        JOIN zastavkaspoje ON zastavkaspoje.cisloSpoje = spoj.cisloSpoje AND zastavkaspoje.linka = spoj.linka
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.linka = zastavkaspoje.linka
        WHERE spoj.id = :spojId
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun spojSeZastavkamiPodleId(spojId: String, pozitivni: Smer = Smer.POZITIVNI, nikdy: Cas = Cas.nikdy): Map<Spoj, List<NazevACas>>

    @Insert
    suspend fun vlozitZastavkySpoje(vararg zastavky: ZastavkaSpoje)

    @Insert
    suspend fun vlozitZastavky(vararg zastavky: Zastavka)

    @Insert
    suspend fun vlozitCasKody(vararg kody: CasKod)

    @Insert
    suspend fun vlozitLinky(vararg linky: Linka)

    @Insert
    suspend fun vlozitSpoje(vararg spoje: Spoj)
}

