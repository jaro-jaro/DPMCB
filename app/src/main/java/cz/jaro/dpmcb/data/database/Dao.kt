package cz.jaro.dpmcb.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.helperclasses.Direction
import cz.jaro.dpmcb.data.realtions.Codes
import cz.jaro.dpmcb.data.realtions.ConnStopWithConnAndCodes
import cz.jaro.dpmcb.data.realtions.LineLowFloorSeqTimeNameConnIdCodes
import cz.jaro.dpmcb.data.realtions.LineLowFloorSeqTimeNameConnIdCodesTab
import cz.jaro.dpmcb.data.realtions.NameAndTime
import cz.jaro.dpmcb.data.realtions.NameTimeIndexOnLine
import cz.jaro.dpmcb.data.realtions.TimeLowFloorConnIdDestinationFixedCodesDelay
import cz.jaro.dpmcb.data.realtions.TimeOfSequence
import cz.jaro.dpmcb.data.realtions.Validity
import java.time.LocalDate

@Dao
interface Dao {
    @Query(
        """
        SELECT DISTINCT stopName FROM stop
        WHERE tab IN (:tabs)
        ORDER BY stopName
    """
    )
    suspend fun stopNames(tabs: List<String>): List<String>

    @Query(
        """
        SELECT DISTINCT shortNumber FROM line
        WHERE tab IN (:tabs)
        ORDER BY shortNumber
    """
    )
    suspend fun lineNumbers(tabs: List<String>): List<Int>

    @Query(
        """
        WITH hereRunningConns AS (
            SELECT DISTINCT conn.* FROM connstop
            JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            WHERE stop.stopName = :thisStop
            AND connstop.line = :line
            AND NOT connstop.departure IS null
        ),
        indexesOfThisStop AS (
            SELECT DISTINCT connstop.stopIndexOnLine, departure, connstop.connNumber, connstop.tab FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab =  connstop.tab
            WHERE connstop.line = :line
            AND stop.stopName = :thisStop
            AND NOT connstop.departure IS null
            ORDER BY connstop.stopIndexOnLine 
        ), 
        negative(max, stopName, indexOnLine, line, connNumber) AS (
            SELECT DISTINCT MAX(connstop.stopIndexOnLine), stop.stopName, connstop.stopIndexOnLine, :line, conn.connNumber
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            JOIN indexesOfThisStop tahleZastavka ON tahleZastavka.connNumber = connstop.connNumber AND tahleZastavka.tab = connstop.tab
            WHERE connstop.stopIndexOnLine < tahleZastavka.stopIndexOnLine
            AND conn.tab = :tab
            AND conn.direction <> :positive
            AND NOT connstop.arrival IS null
            AND NOT tahleZastavka.departure IS null
            GROUP BY conn.connNumber, tahleZastavka.stopIndexOnLine
            ORDER BY -connstop.stopIndexOnLine
        ),
        positive(min, stopName, indexOnLine, line, connNumber) AS (
            SELECT DISTINCT MIN(connstop.stopIndexOnLine), stop.stopName, connstop.stopIndexOnLine, :line, conn.connNumber
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            JOIN indexesOfThisStop tahleZastavka ON tahleZastavka.connNumber = connstop.connNumber AND tahleZastavka.tab = connstop.tab
            WHERE connstop.stopIndexOnLine > tahleZastavka.stopIndexOnLine
            AND conn.tab = :tab
            AND conn.direction = :positive
            AND NOT connstop.arrival IS null
            AND NOT tahleZastavka.departure IS null
            GROUP BY conn.connNumber, tahleZastavka.stopIndexOnLine
            ORDER BY connstop.stopIndexOnLine
        )
        SELECT DISTINCT stopName
        FROM positive
        UNION
        SELECT DISTINCT stopName
        FROM negative
    """
    )
    suspend fun nextStops(line: Int, thisStop: String, tab: String, positive: Direction = Direction.POSITIVE): List<String>

    @Query(
        """
        SELECT DISTINCT stop.stopName FROM connstop
        JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
        JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
        WHERE connstop.line = :line
        AND conn.tab = :tab
        ORDER BY connstop.stopIndexOnLine
    """
    )
    suspend fun stopNamesOfLine(line: Int, tab: String): List<String>

    @Query(
        """
        WITH TimeCodesCountOfConn AS (
            SELECT DISTINCT conn.id, COUNT(timecode.termIndex) count FROM connstop
            JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN timecode ON timecode.connNumber = connstop.connNumber AND timecode.tab = connstop.tab
            WHERE stop.stopName = :stop
            AND conn.line = :line
            AND conn.tab = :tab
            AND (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            GROUP BY conn.id
        ),
        hereRunningConns AS (
            SELECT DISTINCT conn.*, COUNT(timecode.termIndex) FROM connstop
            JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN timecode ON timecode.connNumber = connstop.connNumber AND timecode.tab = connstop.tab
            JOIN TimeCodesCountOfConn ON TimeCodesCountOfConn.id = conn.id
            WHERE stop.stopName = :stop
            AND conn.line = :line
            AND conn.tab = :tab
            AND (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND ((
                timecode.runs 
                AND timecode.validFrom <= :date
                AND :date <= timecode.validTo
            ) OR (
                NOT timecode.runs 
                AND NOT (
                    timecode.validFrom <= :date
                    AND :date <= timecode.validTo
                )
            ))
            GROUP BY conn.id
            HAVING (
                timecode.runs 
                AND COUNT(timecode.termIndex) >= 1
            ) OR (
                NOT timecode.runs
                AND COUNT(timecode.termIndex) = TimeCodesCountOfConn.count
            )
        ),
        thisStopIndex AS (
            SELECT DISTINCT connstop.stopIndexOnLine FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab =  connstop.tab
            WHERE connstop.line = :line
            AND conn.tab = :tab
            AND stop.stopName = :stop
            ORDER BY connstop.stopIndexOnLine
        ),
        negative(max, stopName, indexOnLine, line, connNumber, indexOnThisLine) AS (
            SELECT DISTINCT MAX(connstop.stopIndexOnLine), stop.stopName, connstop.stopIndexOnLine, :line, conn.connNumber, tahleZastavka.stopIndexOnLine
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            CROSS JOIN thisStopIndex tahleZastavka
            WHERE connstop.stopIndexOnLine < tahleZastavka.stopIndexOnLine
            AND conn.tab = :tab
            AND conn.direction <> :positive
            AND (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            GROUP BY conn.connNumber, tahleZastavka.stopIndexOnLine
            ORDER BY -connstop.stopIndexOnLine
        ),
        positive(min, stopName, indexOnLine, line, connNumber, indexOnThisLine) AS (
            SELECT DISTINCT MIN(connstop.stopIndexOnLine), stop.stopName, connstop.stopIndexOnLine, :line, conn.connNumber, tahleZastavka.stopIndexOnLine
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            CROSS JOIN thisStopIndex tahleZastavka
            WHERE connstop.stopIndexOnLine > tahleZastavka.stopIndexOnLine
            AND conn.tab = :tab
            AND conn.direction = :positive
            AND (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            GROUP BY conn.connNumber, tahleZastavka.stopIndexOnLine
            ORDER BY connstop.stopIndexOnLine
        ),
        endStopIndexOnThisLine(connNumber, time, name) AS (
            SELECT DISTINCT connstop.connNumber, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END, stop.stopName FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            WHERE (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND connstop.line = :line
            AND connstop.tab = :tab
            GROUP BY connstop.connNumber
            HAVING MAX(CASE
                WHEN conn.direction = 1 THEN -connstop.stopIndexOnLine
                ELSE connstop.stopIndexOnLine
            END)
        )
        SELECT DISTINCT connstop.departure, (conn.fixedCodes LIKE '%24%') lowFloor, conn.id connId, conn.fixedCodes, endStopIndexOnThisLine.name destination FROM connstop
        JOIN endStopIndexOnThisLine ON endStopIndexOnThisLine.connNumber = connstop.connNumber
        JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
        JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
        CROSS JOIN thisStopIndex tahleZastavka ON connstop.stopIndexOnLine = tahleZastavka.stopIndexOnLine
        LEFT JOIN positive ON positive.connNumber = conn.connNumber AND positive.indexOnThisLine = connstop.stopIndexOnLine
        LEFT JOIN negative ON negative.connNumber = conn.connNumber AND negative.indexOnThisLine = connstop.stopIndexOnLine
        WHERE (conn.direction = :positive AND positive.stopName = :nextStop)
        OR (conn.direction <> :positive AND negative.stopName = :nextStop)
        AND NOT connstop.departure IS null
        AND conn.tab = :tab
        GROUP BY conn.connNumber
    """
    )
    suspend fun connStopsOnLineWithNextStopAtDate(
        line: Int,
        stop: String,
        nextStop: String,
        date: LocalDate,
        tab: String,
        positive: Direction = Direction.POSITIVE,
    ): List<TimeLowFloorConnIdDestinationFixedCodesDelay>

    @Transaction
    @Query(
        """
        SELECT (conn.fixedCodes LIKE '%24%') lowFloor, conn.line, conn.fixedCodes, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stopName name, conn.sequence, conn.id connId, timecode.runs, timecode.validFrom `from`, timecode.validTo `to` FROM connstop
        JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
        JOIN stop ON stop.tab = connstop.tab AND stop.stopNumber = connstop.stopNumber 
        JOIN timecode ON timecode.tab = connstop.tab AND timecode.connNumber = connstop.connNumber 
        WHERE (
            NOT connstop.departure IS null
            OR NOT connstop.arrival IS null
        )
        AND conn.id = :connId
        AND conn.tab = :tab
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun connWithItsConnStopsAndCodes(connId: String, tab: String, positive: Direction = Direction.POSITIVE): List<LineLowFloorSeqTimeNameConnIdCodes>

    @Query(
        """
        SELECT conn.fixedCodes, timecode.runs, timecode.validFrom `from`, timecode.validTo `to` FROM timecode
        JOIN conn ON conn.tab = timecode.tab AND conn.connNumber = timecode.connNumber
        AND conn.id = :connId
        AND conn.tab = :tab
    """
    )
    suspend fun codes(connId: String, tab: String): List<Codes>

    @Transaction
    @Query(
        """
        SELECT (conn.fixedCodes LIKE '%24%') lowFloor, conn.line, conn.sequence, conn.fixedCodes, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stopName name, conn.id connId, conn.tab, timecode.runs, timecode.validFrom `from`, timecode.validTo `to` FROM connstop
        JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
        JOIN stop ON stop.tab = connstop.tab AND stop.stopNumber = connstop.stopNumber 
        JOIN timecode ON timecode.tab = connstop.tab AND timecode.connNumber = connstop.connNumber 
        WHERE (
            NOT connstop.departure IS null
            OR NOT connstop.arrival IS null
        )
        AND (
            conn.sequence LIKE :seq1
            OR conn.sequence LIKE :seq2
            OR conn.sequence LIKE :seq3
        )
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun connsOfSeqWithTheirConnStops(seq1: String, seq2: String, seq3: String, positive: Direction = Direction.POSITIVE): List<LineLowFloorSeqTimeNameConnIdCodesTab>

    @Query(
        """
        WITH hereRunningConns AS (
            SELECT DISTINCT conn.* FROM connstop
            JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            WHERE stop.stopName = :stop
            AND conn.tab IN (:tabs)
            AND (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
        ),
        thisStopIndexes AS (
            SELECT DISTINCT connstop.stopIndexOnLine, connstop.tab FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab =  connstop.tab
            AND stop.stopName = :stop
            AND conn.tab IN (:tabs)
            ORDER BY connstop.stopIndexOnLine
        ),
        thisStop AS (
            SELECT stop.stopName name, conn.fixedCodes, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END time, connstop.stopIndexOnLine, conn.connNumber, conn.line, conn.tab, (conn.fixedCodes LIKE '%24%') lowFloor 
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            CROSS JOIN thisStopIndexes tahleZastavka ON connstop.tab = tahleZastavka.tab AND connstop.stopIndexOnLine = tahleZastavka.stopIndexOnLine
        )
        SELECT thisStop.*, timecode.runs, timecode.validFrom `from`, timecode.validTo `to`
        FROM thisStop
        JOIN hereRunningConns conn ON conn.connNumber = thisStop.connNumber AND conn.tab = thisStop.tab
        JOIN timecode ON timecode.connNumber = thisStop.connNumber AND timecode.tab = thisStop.tab
    """
    )
    suspend fun connsStoppingOnStopName(
        stop: String,
        tabs: List<String>,
    ): List<ConnStopWithConnAndCodes>

    @Query(
        """
        SELECT DISTINCT conn.sequence FROM conn
        WHERE 0
        OR conn.sequence LIKE :sequence1
        OR conn.sequence LIKE :sequence2
        OR conn.sequence LIKE :sequence3
        OR conn.sequence LIKE :sequence4
        OR conn.sequence LIKE :sequence5
        OR conn.sequence LIKE :sequence6
        OR conn.sequence LIKE :sequence7
        OR conn.sequence LIKE :sequence8
        OR conn.sequence LIKE :sequence9
        OR conn.sequence LIKE :sequence10
        OR conn.sequence LIKE :sequence11
        OR conn.sequence LIKE :sequence12
        OR conn.sequence LIKE :sequence13
        OR conn.sequence LIKE :sequence14
        OR conn.sequence LIKE :sequence15
        OR conn.sequence LIKE :sequence16
        OR conn.sequence LIKE :sequence17
        OR conn.sequence LIKE :sequence18
    """
    )
    suspend fun findSequences(
        sequence1: String,
        sequence2: String,
        sequence3: String,
        sequence4: String,
        sequence5: String,
        sequence6: String,
        sequence7: String,
        sequence8: String,
        sequence9: String,
        sequence10: String,
        sequence11: String,
        sequence12: String,
        sequence13: String,
        sequence14: String,
        sequence15: String,
        sequence16: String,
        sequence17: String,
        sequence18: String,
    ): List<String>

    @Transaction
    @Query(
        """
        SELECT DISTINCT conn.sequence, conn.line - 325000 line FROM conn
        WHERE conn.sequence IN (:todayRunningSequences)
    """
    )
    suspend fun sequenceLines(todayRunningSequences: List<String>): Map<@MapColumn("sequence") String, List<@MapColumn("line") Int>>
    @Transaction
    @Query(
        """
        WITH CounOfConnsInSequence AS (
            SELECT COUNT(DISTINCT connNumber) count, sequence
            FROM conn
            GROUP BY sequence
        ),
        TimeCodesOfSeq AS (
            SELECT conn.sequence, timecode.*
            FROM timecode
            JOIN conn ON conn.tab = timecode.tab AND conn.connNumber = timecode.connNumber
            JOIN counofconnsinsequence c ON c.sequence = conn.sequence
            GROUP BY validFrom || validTo || runs, conn.sequence
            HAVING COUNT(DISTINCT conn.connNumber) =  c.count
            ORDER BY conn.sequence, runs, validTo, validFrom
        ),
        TimeCodesCountOfSeq AS (
            SELECT DISTINCT sequence, COUNT(*) count FROM TimeCodesOfSeq timecode
            GROUP BY sequence
        ),
        TodayRunningSequences AS (
            SELECT DISTINCT timecode.sequence FROM TimeCodesOfSeq timecode
            JOIN TimeCodesCountOfSeq ON TimeCodesCountOfSeq.sequence = timecode.sequence
            AND ((
                timecode.runs 
                AND timecode.validFrom <= :date
                AND :date <= timecode.validTo
            ) OR (
                NOT timecode.runs 
                AND NOT (
                    timecode.validFrom <= :date
                    AND :date <= timecode.validTo
                )
            ))
            GROUP BY timecode.sequence
            HAVING (
                timecode.runs 
                AND COUNT(*) >= 1
            ) OR (
                NOT timecode.runs
                AND COUNT(*) = TimeCodesCountOfSeq.count
            )
        ),
        endStopIndexOnThisLine(sequence, time, name) AS (
            SELECT DISTINCT conn.sequence, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END, stop.stopName FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            WHERE (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND connstop.tab IN (:tabs)
            GROUP BY conn.sequence
            HAVING MAX(CASE
                 WHEN connstop.departure IS null THEN connstop.arrival
                 ELSE connstop.departure
             END)
        ),
        startStopIndexOnThisLine(sequence, time, name) AS (
            SELECT DISTINCT conn.sequence, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END, stop.stopName FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            WHERE (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND connstop.tab IN (:tabs)
            GROUP BY conn.sequence
            HAVING MIN(CASE
                 WHEN connstop.departure IS null THEN connstop.arrival
                 ELSE connstop.departure
             END)
        )
        SELECT conn.sequence, conn.fixedCodes, startStopIndexOnThisLine.time start, endStopIndexOnThisLine.time `end` FROM conn
        JOIN startStopIndexOnThisLine ON startStopIndexOnThisLine.sequence = conn.sequence
        JOIN endStopIndexOnThisLine ON endStopIndexOnThisLine.sequence = conn.sequence
        WHERE conn.sequence IN TodayRunningSequences
        AND conn.tab IN (:tabs)
    """ // DISTINCT zde není schválně - chceme pevné kódy pro každý spoj zvlášť, abychom poté získali společné pevné kódy
    )
    suspend fun fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
        date: LocalDate,
        tabs: List<String>
    ): Map<TimeOfSequence, List<@MapColumn("fixedCodes")String>>

//    SELECT group_ID
//    FROM tableName
//    GROUP BY group_ID
//    HAVING COUNT(*) = (SELECT COUNT(DISTINCT uid) AS c FROM tableName);

    @Transaction
    @Query(
        """
        SELECT conn.*, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stop.stopName name FROM conn
        JOIN connstop ON connstop.connNumber = conn.connNumber AND connstop.tab = conn.tab
        JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
        WHERE conn.id = :connId
        AND conn.tab = :tab
        AND (
            NOT connstop.departure IS null
            OR NOT connstop.arrival IS null
        )
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun connWithItsStops(connId: String, tab: String, positive: Direction = Direction.POSITIVE): Map<Conn, List<NameAndTime>>

    @Query(
        """
        SELECT DISTINCT conn.line - 325000 FROM conn
        GROUP BY conn.line
        HAVING COUNT(DISTINCT conn.direction) = 1
    """
    )
    suspend fun oneDirectionLines(): List<Int>

    @Transaction
    @Query(
        """
        SELECT conn.id, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stop.stopName name, connstop.stopIndexOnLine FROM conn
        JOIN connstop ON connstop.connNumber = conn.connNumber AND connstop.tab = conn.tab
        JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
        WHERE conn.id IN (:connIds)
        AND conn.tab IN (:tabs)
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun connStops(connIds: List<String>, tabs: List<String>, positive: Direction = Direction.POSITIVE): Map<@MapColumn("id") String, List<NameTimeIndexOnLine>>

    @Query(
        """
        SELECT hasRestriction FROM line
        WHERE tab = :tab
        LIMIT 1
    """
    )
    suspend fun hasRestriction(tab: String): Boolean

    @Query(
        """
        SELECT validFrom, validTo FROM line
        WHERE tab = :tab
        LIMIT 1
    """
    )
    suspend fun validity(tab: String): Validity

    @Query(
        """
        SELECT id FROM conn
        WHERE id = :connId
        LIMIT 1
    """
    )
    suspend fun doesConnExist(connId: String): String?

    @Query(
        """
        SELECT * FROM line
        WHERE number = :line
    """
    )
    suspend fun lineTables(line: Int): List<Line>

    @Query(
        """
        SELECT DISTINCT number FROM line
    """
    )
    suspend fun allLineNumbers(): List<Int>

    @Insert
    suspend fun insertConnStops(vararg connStops: ConnStop)

    @Insert
    suspend fun insertStops(vararg stops: Stop)

    @Insert
    suspend fun insertTimeCodes(vararg timeCodes: TimeCode)

    @Insert
    suspend fun insertLines(vararg lines: Line)

    @Insert
    suspend fun insertConns(vararg conns: Conn)

    @Query("SELECT * FROM connstop")
    suspend fun connStops(): List<ConnStop>

    @Query("SELECT * FROM stop")
    suspend fun stops(): List<Stop>

    @Query("SELECT * FROM timecode")
    suspend fun timeCodes(): List<TimeCode>

    @Query("SELECT * FROM line")
    suspend fun lines(): List<Line>

    @Query("SELECT * FROM conn")
    suspend fun conns(): List<Conn>
}