package cz.jaro.dpmcb.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import cz.jaro.dpmcb.data.entities.CasKod
import cz.jaro.dpmcb.data.entities.Linka
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.entities.Zastavka
import cz.jaro.dpmcb.data.entities.ZastavkaSpoje
import cz.jaro.dpmcb.data.helperclasses.Smer
import cz.jaro.dpmcb.data.realtions.CaskodSPevnymiKody
import cz.jaro.dpmcb.data.realtions.Kody
import cz.jaro.dpmcb.data.realtions.LinkaNizkopodlaznostCasNazevSpojId
import cz.jaro.dpmcb.data.realtions.LinkaNizkopodlaznostCasNazevSpojIdTabulka
import cz.jaro.dpmcb.data.realtions.NazevACas
import cz.jaro.dpmcb.data.realtions.NazevCasIndexNaLince
import cz.jaro.dpmcb.data.realtions.OdjezdNizkopodlaznostSpojId
import cz.jaro.dpmcb.data.realtions.Platnost
import cz.jaro.dpmcb.data.realtions.SpojKurzAKody
import cz.jaro.dpmcb.data.realtions.ZastavkaSpojeSeSpojem
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface Dao {
    @Query(
        """
        SELECT DISTINCT nazevZastavky FROM zastavka
        WHERE tab IN (:tabs)
        ORDER BY nazevZastavky
    """
    )
    suspend fun nazvyZastavek(tabs: List<String>): List<String>

    @Query(
        """
        SELECT DISTINCT kratkeCislo FROM linka
        WHERE tab IN (:tabs)
        ORDER BY kratkeCislo
    """
    )
    suspend fun cislaLinek(tabs: List<String>): List<Int>

    @Query(
        """
        WITH spojeZdeJedouci AS (
            SELECT DISTINCT spoj.* FROM zastavkaspoje
            JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            WHERE zastavka.nazevZastavky = :tahleZastavka
            AND zastavkaspoje.linka = :linka
            AND NOT zastavkaspoje.odjezd IS null
        ),
        indexyTyhleZastavky AS (
            SELECT DISTINCT zastavkaSpoje.indexZastavkyNaLince, odjezd, zastavkaspoje.cisloSpoje, zastavkaspoje.tab FROM zastavkaSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab =  zastavkaspoje.tab
            WHERE zastavkaSpoje.linka = :linka
            AND zastavka.nazevZastavky = :tahleZastavka
            AND NOT zastavkaspoje.odjezd IS null
            ORDER BY zastavkaSpoje.indexZastavkyNaLince 
        ), 
        negativni(max, nazevZastavky, indexNaLince, linka, cisloSpoje) AS (
            SELECT DISTINCT MAX(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
            JOIN indexyTyhleZastavky tahleZastavka ON tahleZastavka.cisloSpoje = zastavkaspoje.cisloSpoje AND tahleZastavka.tab = zastavkaspoje.tab
            WHERE zastavkaspoje.indexZastavkyNaLince < tahleZastavka.indexZastavkyNaLince
            AND spoj.tab = :tab
            AND spoj.smer <> :pozitivni
            AND NOT zastavkaspoje.prijezd IS null
            AND NOT tahleZastavka.odjezd IS null
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY -zastavkaSpoje.indexZastavkyNaLince
        ),
        pozitivni(min, nazevZastavky, indexNaLince, linka, cisloSpoje) AS (
            SELECT DISTINCT MIN(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
            JOIN indexyTyhleZastavky tahleZastavka ON tahleZastavka.cisloSpoje = zastavkaspoje.cisloSpoje AND tahleZastavka.tab = zastavkaspoje.tab
            WHERE zastavkaspoje.indexZastavkyNaLince > tahleZastavka.indexZastavkyNaLince
            AND spoj.tab = :tab
            AND spoj.smer = :pozitivni
            AND NOT zastavkaspoje.prijezd IS null
            AND NOT tahleZastavka.odjezd IS null
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
    suspend fun pristiZastavky(linka: Int, tahleZastavka: String, tab: String, pozitivni: Smer = Smer.POZITIVNI): List<String>

    @Query(
        """
        SELECT DISTINCT zastavka.nazevZastavky FROM zastavkaSpoje
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaSpoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
        JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
        WHERE zastavkaSpoje.linka = :linka
        AND spoj.tab = :tab
        ORDER BY zastavkaSpoje.indexZastavkyNaLince
    """
    )
    suspend fun nazvyZastavekLinky(linka: Int, tab: String): List<String>

    @Query(
        """
        WITH pocetCaskoduSpoje AS (
            SELECT DISTINCT spoj.id, COUNT(caskod.indexTerminu) pocet FROM zastavkaspoje
            JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN caskod ON caskod.cisloSpoje = zastavkaspoje.cisloSpoje AND caskod.tab = zastavkaspoje.tab
            WHERE zastavka.nazevZastavky = :zastavka
            AND spoj.linka = :linka
            AND spoj.tab = :tab
            AND (
                NOT zastavkaspoje.odjezd IS null
                OR NOT zastavkaspoje.prijezd IS null
            )
            GROUP BY spoj.id
        ),
        spojeZdeJedouci AS (
            SELECT DISTINCT spoj.*, COUNT(caskod.indexTerminu) FROM zastavkaspoje
            JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN caskod ON caskod.cisloSpoje = zastavkaspoje.cisloSpoje AND caskod.tab = zastavkaspoje.tab
            JOIN pocetCaskoduSpoje ON pocetCaskoduSpoje.id = spoj.id
            WHERE zastavka.nazevZastavky = :zastavka
            AND spoj.linka = :linka
            AND spoj.tab = :tab
            AND (
                NOT zastavkaspoje.odjezd IS null
                OR NOT zastavkaspoje.prijezd IS null
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
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab =  zastavkaspoje.tab
            WHERE zastavkaSpoje.linka = :linka
            AND spoj.tab = :tab
            AND zastavka.nazevZastavky = :zastavka
            ORDER BY zastavkaSpoje.indexZastavkyNaLince
        ),
        negativni(max, nazevZastavky, indexNaLince, linka, cisloSpoje, indexTyhleNaLince) AS (
            SELECT DISTINCT MAX(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
            CROSS JOIN indexyTyhleZastavky tahleZastavka
            WHERE zastavkaspoje.indexZastavkyNaLince < tahleZastavka.indexZastavkyNaLince
            AND spoj.tab = :tab
            AND spoj.smer <> :pozitivni
            AND (
                NOT zastavkaspoje.odjezd IS null
                OR NOT zastavkaspoje.prijezd IS null
            )
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY -zastavkaSpoje.indexZastavkyNaLince
        ),
        pozitivni(min, nazevZastavky, indexNaLince, linka, cisloSpoje, indexTyhleNaLince) AS (
            SELECT DISTINCT MIN(zastavkaSpoje.indexZastavkyNaLince), zastavka.nazevZastavky, zastavkaspoje.indexZastavkyNaLince, :linka, spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
            CROSS JOIN indexyTyhleZastavky tahleZastavka
            WHERE zastavkaspoje.indexZastavkyNaLince > tahleZastavka.indexZastavkyNaLince
            AND spoj.tab = :tab
            AND spoj.smer = :pozitivni
            AND (
                NOT zastavkaspoje.odjezd IS null
                OR NOT zastavkaspoje.prijezd IS null
            )
            GROUP BY spoj.cisloSpoje, tahleZastavka.indexZastavkyNaLince
            ORDER BY zastavkaSpoje.indexZastavkyNaLince
        ),
        konecna(cisloSpoje, cas, nazev) AS (
            SELECT DISTINCT zastavkaspoje.cisloSpoje, CASE
                WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
                ELSE zastavkaspoje.odjezd
            END, zastavka.nazevZastavky FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
            WHERE (
                NOT zastavkaspoje.odjezd IS null
                OR NOT zastavkaspoje.prijezd IS null
            )
            AND zastavkaSpoje.linka = :linka
            AND zastavkaSpoje.tab = :tab
            GROUP BY zastavkaspoje.cisloSpoje
            HAVING MAX(CASE
                WHEN spoj.smer = 1 THEN -zastavkaSpoje.indexZastavkyNaLince
                ELSE zastavkaSpoje.indexZastavkyNaLince
            END)
        )
        SELECT DISTINCT zastavkaspoje.odjezd, (spoj.pevnekody LIKE '%24%') nizkopodlaznost, spoj.id spojId, spoj.pevneKody, konecna.nazev cil FROM zastavkaspoje
        JOIN konecna ON konecna.cisloSpoje = zastavkaspoje.cisloSpoje
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
        JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
        CROSS JOIN indexyTyhleZastavky tahleZastavka ON zastavkaspoje.indexZastavkyNaLince = tahleZastavka.indexZastavkyNaLince
        LEFT JOIN pozitivni ON pozitivni.cisloSpoje = spoj.cisloSpoje AND pozitivni.indexTyhleNaLince = zastavkaspoje.indexZastavkyNaLince
        LEFT JOIN negativni ON negativni.cisloSpoje = spoj.cisloSpoje AND negativni.indexTyhleNaLince = zastavkaspoje.indexZastavkyNaLince
        WHERE (spoj.smer = :pozitivni AND pozitivni.nazevZastavky = :pristiZastavka)
        OR (spoj.smer <> :pozitivni AND negativni.nazevZastavky = :pristiZastavka)
        AND NOT zastavkaspoje.odjezd IS null
        AND spoj.tab = :tab
        GROUP BY spoj.cisloSpoje
    """
    )
    suspend fun zastavkyJedouciVDatumSPristiZastavkou(
        linka: Int,
        zastavka: String,
        pristiZastavka: String,
        datum: LocalDate,
        tab: String,
        pozitivni: Smer = Smer.POZITIVNI,
    ): List<OdjezdNizkopodlaznostSpojId>

    @Transaction
    @Query(
        """
        SELECT (spoj.pevnekody LIKE '%24%') nizkopodlaznost, spoj.linka, spoj.pevneKody, CASE
            WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END cas, nazevZastavky nazev, spoj.kurz, spoj.id spojId, caskod.jede, caskod.platiOd od, caskod.platiDo `do` FROM zastavkaspoje
        JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
        JOIN zastavka ON zastavka.tab = zastavkaspoje.tab AND zastavka.cisloZastavky = zastavkaspoje.cisloZastavky 
        JOIN caskod ON caskod.tab = zastavkaspoje.tab AND caskod.cisloSpoje = zastavkaspoje.cisloSpoje 
        WHERE (
            NOT zastavkaspoje.odjezd IS null
            OR NOT zastavkaspoje.prijezd IS null
        )
        AND spoj.id = :spojId
        AND spoj.tab = :tab
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun spojSeZastavkySpojeNaKterychStavi(spojId: String, tab: String, pozitivni: Smer = Smer.POZITIVNI): List<LinkaNizkopodlaznostCasNazevSpojId>

    @Query(
        """
        SELECT spoj.pevneKody, caskod.jede, caskod.platiOd od, caskod.platiDo `do` FROM caskod
        JOIN spoj ON spoj.tab = caskod.tab AND spoj.cisloSpoje = caskod.cisloSpoje
        AND spoj.id = :spojId
        AND spoj.tab = :tab
    """
    )
    suspend fun kody(spojId: String, tab: String): List<Kody>

    @Transaction
    @Query(
        """
        SELECT (spoj.pevnekody LIKE '%24%') nizkopodlaznost, spoj.linka, spoj.kurz, spoj.pevneKody, CASE
            WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END cas, nazevZastavky nazev, spoj.id spojId, spoj.tab, caskod.jede, caskod.platiOd od, caskod.platiDo `do` FROM zastavkaspoje
        JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
        JOIN zastavka ON zastavka.tab = zastavkaspoje.tab AND zastavka.cisloZastavky = zastavkaspoje.cisloZastavky 
        JOIN caskod ON caskod.tab = zastavkaspoje.tab AND caskod.cisloSpoje = zastavkaspoje.cisloSpoje 
        WHERE (
            NOT zastavkaspoje.odjezd IS null
            OR NOT zastavkaspoje.prijezd IS null
        )
        AND (
            spoj.kurz LIKE :kurz1
            OR spoj.kurz LIKE :kurz2
            OR spoj.kurz LIKE :kurz3
        )
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun spojeKurzuSeZastavkySpojeNaKterychStavi(kurz1: String, kurz2: String, kurz3: String, pozitivni: Smer = Smer.POZITIVNI): List<LinkaNizkopodlaznostCasNazevSpojIdTabulka>

    @Transaction
    @Query(
        """
        SELECT spoj.pevneKody, caskod.jede, caskod.platiOd od, caskod.platiDo `do` FROM caskod
        JOIN spoj ON spoj.tab = caskod.tab AND spoj.cisloSpoje = caskod.cisloSpoje
        WHERE spoj.id = :spojId
        AND spoj.tab = :tab
    """
    )
    suspend fun pevneKodyCaskody(spojId: String, tab: String): List<CaskodSPevnymiKody>

    @Query(
        """
        WITH spojeZdeJedouci AS (
            SELECT DISTINCT spoj.* FROM zastavkaspoje
            JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            WHERE zastavka.nazevZastavky = :zastavka
            AND spoj.tab IN (:tabs)
            AND (
                NOT zastavkaspoje.odjezd IS null
                OR NOT zastavkaspoje.prijezd IS null
            )
        ),
        indexyTyhleZastavky AS (
            SELECT DISTINCT zastavkaSpoje.indexZastavkyNaLince, zastavkaspoje.tab FROM zastavkaSpoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab =  zastavkaspoje.tab
            AND zastavka.nazevZastavky = :zastavka
            AND spoj.tab IN (:tabs)
            ORDER BY zastavkaSpoje.indexZastavkyNaLince
        ),
        tahleZastavka AS (
            SELECT zastavka.nazevZastavky nazev, spoj.pevneKody, CASE
                WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
                ELSE zastavkaspoje.odjezd
            END cas, zastavkaspoje.indexZastavkyNaLince, spoj.cisloSpoje, spoj.linka, spoj.tab, (spoj.pevnekody LIKE '%24%') nizkopodlaznost 
            FROM zastavkaspoje
            JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
            JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = zastavkaspoje.cisloSpoje AND spoj.tab = zastavkaspoje.tab
            CROSS JOIN indexyTyhleZastavky tahleZastavka ON zastavkaspoje.tab = tahleZastavka.tab AND zastavkaspoje.indexZastavkyNaLince = tahleZastavka.indexZastavkyNaLince
        )
        SELECT tahleZastavka.*, caskod.jede, caskod.platiOd od, caskod.platiDo `do`
        FROM tahleZastavka
        JOIN spojeZdeJedouci spoj ON spoj.cisloSpoje = tahleZastavka.cisloSpoje AND spoj.tab = tahleZastavka.tab
        JOIN caskod ON caskod.cisloSpoje = tahleZastavka.cisloSpoje AND caskod.tab = tahleZastavka.tab
    """
    )
    suspend fun spojeZastavujiciNaIndexechZastavky(
        zastavka: String,
        tabs: List<String>,
    ): List<ZastavkaSpojeSeSpojem>

    @Query(
        """
        SELECT DISTINCT spoj.kurz FROM spoj
        WHERE spoj.kurz LIKE :kurz1
        OR spoj.kurz LIKE :kurz2
        OR spoj.kurz LIKE :kurz3
        OR spoj.kurz LIKE :kurz4
        OR spoj.kurz LIKE :kurz5
        OR spoj.kurz LIKE :kurz6
        OR spoj.kurz LIKE :kurz7
        OR spoj.kurz LIKE :kurz8
        OR spoj.kurz LIKE :kurz9
        OR spoj.kurz LIKE :kurz10
        OR spoj.kurz LIKE :kurz11
        OR spoj.kurz LIKE :kurz12
        OR spoj.kurz LIKE :kurz13
        OR spoj.kurz LIKE :kurz14
        OR spoj.kurz LIKE :kurz15
        OR spoj.kurz LIKE :kurz16
        OR spoj.kurz LIKE :kurz17
        OR spoj.kurz LIKE :kurz18
    """
    )
    suspend fun hledatKurzy(
        kurz1: String,
        kurz2: String,
        kurz3: String,
        kurz4: String,
        kurz5: String,
        kurz6: String,
        kurz7: String,
        kurz8: String,
        kurz9: String,
        kurz10: String,
        kurz11: String,
        kurz12: String,
        kurz13: String,
        kurz14: String,
        kurz15: String,
        kurz16: String,
        kurz17: String,
        kurz18: String,
    ): List<String>

    @Transaction
    @Query(
        """
        SELECT DISTINCT spoj.kurz, spoj.linka FROM spoj
        JOIN (
            SELECT DISTINCT CASE
                WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
                ELSE zastavkaspoje.odjezd
            END cas, spoj.kurz FROM zastavkaspoje
            JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
        ) druhazastavka ON spoj.kurz = druhazastavka.kurz
        JOIN (
            SELECT DISTINCT CASE
                WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
                ELSE zastavkaspoje.odjezd
            END cas, spoj.kurz FROM zastavkaspoje
            JOIN spoj ON spoj.tab = zastavkaspoje.tab AND spoj.cisloSpoje = zastavkaspoje.cisloSpoje
        ) tretizastavka ON spoj.kurz = tretizastavka.kurz
        WHERE druhazastavka.cas < :cas
        AND :cas < tretizastavka.cas
    """
    )
    suspend fun praveJedouci(cas: LocalTime): Map<@MapColumn("kurz") String, List<@MapColumn("linka") Int>>
    @Transaction
    @Query(
        """
        SELECT DISTINCT spoj.kurz, caskod.jede, caskod.platiOd od, caskod.platiDo `do`, spoj.pevneKody, spoj.tab, spoj.id spojId FROM spoj
        JOIN caskod ON caskod.tab = spoj.tab AND caskod.cisloSpoje = spoj.cisloSpoje
    """
    )
    suspend fun kodyKurzu(): Map<@MapColumn("kurz") String, List<SpojKurzAKody>>

    @Transaction
    @Query(
        """
        SELECT spoj.*, CASE
            WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END cas, zastavka.nazevZastavky nazev FROM spoj
        JOIN zastavkaspoje ON zastavkaspoje.cisloSpoje = spoj.cisloSpoje AND zastavkaspoje.tab = spoj.tab
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
        WHERE spoj.id = :spojId
        AND spoj.tab = :tab
        AND (
            NOT zastavkaspoje.odjezd IS null
            OR NOT zastavkaspoje.prijezd IS null
        )
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun spojSeZastavkamiPodleId(spojId: String, tab: String, pozitivni: Smer = Smer.POZITIVNI): Map<Spoj, List<NazevACas>>

    @Query(
        """
        SELECT DISTINCT spoj.linka - 325000 FROM spoj
        GROUP BY spoj.linka
        HAVING COUNT(DISTINCT spoj.smer) = 1
    """
    )
    suspend fun jednosmerneLinky(): List<Int>

    @Transaction
    @Query(
        """
        SELECT spoj.id, CASE
            WHEN zastavkaspoje.odjezd IS null THEN zastavkaspoje.prijezd
            ELSE zastavkaspoje.odjezd
        END cas, zastavka.nazevZastavky nazev, zastavkaspoje.indexZastavkyNaLince FROM spoj
        JOIN zastavkaspoje ON zastavkaspoje.cisloSpoje = spoj.cisloSpoje AND zastavkaspoje.tab = spoj.tab
        JOIN zastavka ON zastavka.cisloZastavky = zastavkaspoje.cisloZastavky AND zastavka.tab = zastavkaspoje.tab
        WHERE spoj.id IN (:spojIds)
        AND spoj.tab IN (:tabs)
        ORDER BY CASE
           WHEN spoj.smer = :pozitivni THEN zastavkaSpoje.indexZastavkyNaLince
           ELSE -zastavkaSpoje.indexZastavkyNaLince
        END
    """
    )
    suspend fun zastavkySpoju(spojIds: List<String>, tabs: List<String>, pozitivni: Smer = Smer.POZITIVNI): Map<@MapColumn("id") String, List<NazevCasIndexNaLince>>

    @Query(
        """
        SELECT maVyluku FROM linka
        WHERE tab = :tab
        LIMIT 1
    """
    )
    suspend fun vyluka(tab: String): Boolean

    @Query(
        """
        SELECT platnostOd, platnostDo FROM linka
        WHERE tab = :tab
        LIMIT 1
    """
    )
    suspend fun platnost(tab: String): Platnost

    @Query(
        """
        SELECT id FROM spoj
        WHERE id = :spojId
        LIMIT 1
    """
    )
    suspend fun existujeSpoj(spojId: String): String?

    @Query(
        """
        SELECT * FROM linka
        WHERE cislo = :linka
    """
    )
    suspend fun tabulkyLinky(linka: Int): List<Linka>

    @Query(
        """
        SELECT DISTINCT cislo FROM linka
    """
    )
    suspend fun vsechnyLinky(): List<Int>

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

    @Query("SELECT * FROM zastavkaspoje")
    suspend fun zastavkySpoje(): List<ZastavkaSpoje>

    @Query("SELECT * FROM zastavka")
    suspend fun zastavky(): List<Zastavka>

    @Query("SELECT * FROM caskod")
    suspend fun casKody(): List<CasKod>

    @Query("SELECT * FROM linka")
    suspend fun linky(): List<Linka>

    @Query("SELECT * FROM spoj")
    suspend fun spoje(): List<Spoj>
}