package cz.jaro.dpmcb.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SeqOfConn
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.SequenceGroup
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.Table
import cz.jaro.dpmcb.data.entities.TimeCode
import cz.jaro.dpmcb.data.entities.Validity
import cz.jaro.dpmcb.data.entities.types.Direction
import cz.jaro.dpmcb.data.realtions.CoreBus
import cz.jaro.dpmcb.data.realtions.bus.CodesOfBus
import cz.jaro.dpmcb.data.realtions.departures.CoreDeparture
import cz.jaro.dpmcb.data.realtions.departures.StopOfDeparture
import cz.jaro.dpmcb.data.realtions.now_running.BusOfNowRunning
import cz.jaro.dpmcb.data.realtions.now_running.StopOfNowRunning
import cz.jaro.dpmcb.data.realtions.sequence.CoreBusOfSequence
import cz.jaro.dpmcb.data.realtions.sequence.TimeOfSequence
import cz.jaro.dpmcb.data.realtions.timetable.CoreBusInTimetable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Dao
interface Dao {

    @Query(
        """
        SELECT number FROM line
        WHERE shortNumber = :line
        LIMIT 1
    """
    )
    suspend fun findLongLine(line: ShortLine): LongLine

    @Query(
        """
        SELECT DISTINCT stopName FROM stop
        WHERE tab IN (:tabs)
        ORDER BY stopName
    """
    )
    suspend fun stopNames(tabs: List<Table>): List<String>

    @Query(
        """
        SELECT DISTINCT shortNumber FROM line
        WHERE tab IN (:tabs)
        ORDER BY shortNumber
    """
    )
    suspend fun lineNumbers(tabs: List<Table>): List<ShortLine>

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
    suspend fun nextStops(line: LongLine, thisStop: String, tab: Table, positive: Direction = Direction.POSITIVE): List<String>

    @Query(
        """
        WITH TimeCodesCountOfConn AS (
            SELECT DISTINCT conn.connNumber, conn.tab, COUNT(timecode.termIndex) count FROM timecode
            JOIN conn ON conn.tab = timecode.tab AND conn.connNumber = timecode.connNumber
            GROUP BY conn.name, conn.tab
        ),
        TodayRunningConns AS (
            SELECT DISTINCT conn.* FROM timecode
            JOIN conn ON conn.tab = timecode.tab AND conn.connNumber = timecode.connNumber
            JOIN TimeCodesCountOfConn ON TimeCodesCountOfConn.connNumber = conn.connNumber AND timecodescountofconn.tab = conn.tab
            WHERE ((
                timecode.type = "RunsOnly"
                AND timecode.validFrom <= :date
                AND :date <= timecode.validTo
            ) OR (
                timecode.type = "RunsAlso"
            ) OR (
                timecode.type = "Runs"
                AND timecode.validFrom <= :date
                AND :date <= timecode.validTo
            ) OR (
                timecode.type = "DoesNotRun"
                AND NOT (
                    timecode.validFrom <= :date
                    AND :date <= timecode.validTo
                )
            ))
            AND conn.tab = :tab
            GROUP BY conn.name, conn.tab
            HAVING (
                timecode.runs2
                AND COUNT(timecode.termIndex) >= 1
            ) OR (
                NOT timecode.runs2
                AND COUNT(timecode.termIndex) = TimeCodesCountOfConn.count
            )
        ),
        thisStopIndex AS (
            SELECT DISTINCT connstop.stopIndexOnLine FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab =  connstop.tab
            WHERE conn.tab = :tab
            AND stop.stopName = :stop
            ORDER BY connstop.stopIndexOnLine
        ),
        negative(max, stopName, indexOnLine, connNumber, indexOnThisLine) AS (
            SELECT DISTINCT MAX(connstop.stopIndexOnLine), stop.stopName, connstop.stopIndexOnLine, conn.connNumber, tahleZastavka.stopIndexOnLine
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN TodayRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            CROSS JOIN thisStopIndex tahleZastavka
            WHERE connstop.stopIndexOnLine < tahleZastavka.stopIndexOnLine
            AND conn.direction <> :positive
            AND (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            GROUP BY conn.connNumber, tahleZastavka.stopIndexOnLine
            ORDER BY -connstop.stopIndexOnLine
        ),
        positive(min, stopName, indexOnLine, connNumber, indexOnThisLine) AS (
            SELECT DISTINCT MIN(connstop.stopIndexOnLine), stop.stopName, connstop.stopIndexOnLine, conn.connNumber, tahleZastavka.stopIndexOnLine
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN TodayRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            CROSS JOIN thisStopIndex tahleZastavka
            WHERE connstop.stopIndexOnLine > tahleZastavka.stopIndexOnLine
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
            AND connstop.tab = :tab
            GROUP BY connstop.connNumber
            HAVING MAX(CASE
                WHEN conn.direction = 1 THEN -connstop.stopIndexOnLine
                ELSE connstop.stopIndexOnLine
            END)
        )
        SELECT DISTINCT connstop.departure, (conn.fixedCodes LIKE '%{%') lowFloor, conn.name connName, conn.fixedCodes, endStopIndexOnThisLine.name destination, timecode.type, timecode.validFrom `from`, timecode.validTo `to` FROM TodayRunningConns conn
        JOIN endStopIndexOnThisLine ON endStopIndexOnThisLine.connNumber = connstop.connNumber
        JOIN connstop ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
        CROSS JOIN thisStopIndex tahleZastavka ON connstop.stopIndexOnLine = tahleZastavka.stopIndexOnLine
        LEFT JOIN positive ON positive.connNumber = conn.connNumber AND positive.indexOnThisLine = connstop.stopIndexOnLine
        LEFT JOIN negative ON negative.connNumber = conn.connNumber AND negative.indexOnThisLine = connstop.stopIndexOnLine
        JOIN timecode ON timecode.connNumber = conn.connNumber AND timecode.tab = conn.tab
        WHERE (conn.direction = :positive AND positive.stopName = :nextStop)
        OR (conn.direction <> :positive AND negative.stopName = :nextStop)
        AND NOT connstop.departure IS null
    """
    )
    suspend fun connStopsOnLineWithNextStopAtDate(
        stop: String,
        nextStop: String,
        date: LocalDate,
        tab: Table,
        positive: Direction = Direction.POSITIVE,
    ): List<CoreBusInTimetable>

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
    suspend fun stopNamesOfLine(line: LongLine, tab: Table): List<String>

    @Transaction
    @Query(
        """
        SELECT (conn.fixedCodes LIKE '%{%') lowFloor, conn.line, conn.fixedCodes, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stop.fixedCodes stopFixedCodes, connstop.fixedCodes connStopFixedCodes, stopName name, seqofconn.sequence, conn.name connName, timecode.type, timecode.validFrom `from`, timecode.validTo `to`, seqofconn.`group` FROM connstop
        JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
        JOIN stop ON stop.tab = connstop.tab AND stop.stopNumber = connstop.stopNumber 
        JOIN timecode ON timecode.tab = connstop.tab AND timecode.connNumber = connstop.connNumber
        JOIN seqofconn ON seqofconn.line = conn.line AND seqofconn.connNumber = conn.connNumber
        WHERE (
            NOT connstop.departure IS null
            OR NOT connstop.arrival IS null
        )
        AND conn.name = :connName
        AND conn.tab = :tab
        AND seqofconn.`group` IN (:groups)
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun coreBus(connName: BusName, groups: List<SequenceGroup>, tab: Table, positive: Direction = Direction.POSITIVE): List<CoreBus>

    @Query(
        """
        SELECT conn.fixedCodes, timecode.type, timecode.validFrom `from`, timecode.validTo `to` FROM timecode
        JOIN conn ON conn.tab = timecode.tab AND conn.connNumber = timecode.connNumber
        AND conn.name = :connName
        AND conn.tab = :tab
    """
    )
    suspend fun codes(connName: BusName, tab: Table): List<CodesOfBus>

    @Transaction
    @Query(
        """
        SELECT (conn.fixedCodes LIKE '%{%') lowFloor, conn.line, seqofconn.sequence, conn.fixedCodes, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stop.fixedCodes stopFixedCodes, connstop.fixedCodes connStopFixedCodes, stopName name, conn.name connName, conn.tab, timecode.type, timecode.validFrom `from`, timecode.validTo `to` FROM connstop
        JOIN conn ON conn.tab = connstop.tab AND conn.connNumber = connstop.connNumber
        JOIN stop ON stop.tab = connstop.tab AND stop.stopNumber = connstop.stopNumber 
        JOIN timecode ON timecode.tab = connstop.tab AND timecode.connNumber = connstop.connNumber 
        JOIN seqofconn ON seqofconn.line = conn.line AND seqofconn.connNumber = conn.connNumber
        WHERE (
            NOT connstop.departure IS null
            OR NOT connstop.arrival IS null
        )
        AND seqofconn.`group` = :group
        AND seqofconn.sequence LIKE :seq
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun coreBusOfSequence(seq: SequenceCode, group: SequenceGroup?, positive: Direction = Direction.POSITIVE): List<CoreBusOfSequence>

    @Transaction
    @Query(
        """
        SELECT DISTINCT conn.name connName FROM conn
        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
        WHERE seqofconn.sequence = :seq
        AND seqofconn.`group` = :group
        AND conn.tab IN (:tabs)
        ORDER BY seqofconn.orderInSequence
    """
    )
    suspend fun connsOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): List<BusName>

    @Transaction
    @Query(
        """
        SELECT DISTINCT conn.name connName FROM conn
        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
        WHERE seqofconn.sequence = :seq
        AND seqofconn.`group` = :group
        AND conn.tab IN (:tabs)
        ORDER BY seqofconn.orderInSequence
        LIMIT 1
    """
    )
    suspend fun firstConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName

    @Transaction
    @Query(
        """
        SELECT DISTINCT conn.name connName FROM conn
        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
        WHERE seqofconn.sequence = :seq
        AND seqofconn.`group` = :group
        AND conn.tab IN (:tabs)
        ORDER BY -seqofconn.orderInSequence
        LIMIT 1
    """
    )
    suspend fun lastConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName

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
            END time, connstop.stopIndexOnLine, connstop.fixedCodes connStopFixedCodes, conn.connNumber, conn.line, conn.tab, (conn.fixedCodes LIKE '%{%') lowFloor 
            FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN hereRunningConns conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            CROSS JOIN thisStopIndexes tahleZastavka ON connstop.tab = tahleZastavka.tab AND connstop.stopIndexOnLine = tahleZastavka.stopIndexOnLine
        )
        SELECT thisStop.*, timecode.type, timecode.validFrom `from`, timecode.validTo `to`
        FROM thisStop
        JOIN hereRunningConns conn ON conn.connNumber = thisStop.connNumber AND conn.tab = thisStop.tab
        JOIN timecode ON timecode.connNumber = thisStop.connNumber AND timecode.tab = thisStop.tab
    """
    )
    suspend fun departures(
        stop: String,
        tabs: List<Table>,
    ): List<CoreDeparture>

    @Query(
        """
        SELECT DISTINCT seqofconn.sequence FROM seqofconn
        WHERE 0
        OR seqofconn.sequence LIKE :sequence1
        OR seqofconn.sequence LIKE :sequence2
        OR seqofconn.sequence LIKE :sequence3
        OR seqofconn.sequence LIKE :sequence4
        OR seqofconn.sequence LIKE :sequence5
        OR seqofconn.sequence LIKE :sequence6
    """
    )
    suspend fun findSequences(
        sequence1: String,
        sequence2: String,
        sequence3: String,
        sequence4: String,
        sequence5: String,
        sequence6: String,
    ): List<SequenceCode>

    @Transaction
    @Query(
        """
        WITH lastStopTimeOfConn(connName, time) AS (
            SELECT DISTINCT conn.name, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END FROM connstop
            JOIN conn ON conn.connNumber = connstop.connNumber AND conn.tab = connstop.tab
            WHERE (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND connstop.tab IN (:tabs)
            GROUP BY conn.name
            HAVING MAX(CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END)
        )
        SELECT DISTINCT seqofconn.sequence, lastStopTimeOfConn.* FROM conn
        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
        JOIN lastStopTimeOfConn ON lastStopTimeOfConn.connName = conn.name
        WHERE seqofconn.sequence IN (:todayRunningSequences)
        AND seqofconn.`group` in (:groups)
        AND conn.tab in (:tabs)
        ORDER BY seqofconn.sequence, lastStopTimeOfConn.time
    """
    )
    suspend fun lastStopTimesOfConnsInSequences(todayRunningSequences: List<SequenceCode>, groups: List<SequenceGroup>, tabs: List<Table>): Map<@MapColumn("sequence") SequenceCode, Map<@MapColumn("connName") BusName, @MapColumn("time") LocalTime>>

    @Transaction
    @Query(
        """
        SELECT conn.name connName, conn.line, conn.direction, conn.tab, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stop.stopName name, seqofconn.sequence FROM conn
        JOIN connstop ON connstop.connNumber = conn.connNumber AND connstop.tab = conn.tab
        JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
        WHERE conn.name = :connName
        AND seqofconn.`group` IN (:groups)
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
    suspend fun connWithItsStops(connName: BusName, groups: List<SequenceGroup>, tab: Table, positive: Direction = Direction.POSITIVE): Map<BusOfNowRunning, List<StopOfNowRunning>>

//    @Transaction
//    @Query(
//        """
//        SELECT seqofconn.sequence FROM conn
//        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
//        WHERE conn.name = :connName
//        AND conn.tab = :tab
//        AND seqofconn.`group` IN (:groups)
//        LIMIT 1
//    """
//    )
//    suspend fun seqOfConn(connName: BusName, tab: Table): SequenceCode

    @Transaction
    @Query(
        """
        WITH CounOfConnsInSequence AS (
            SELECT COUNT(DISTINCT connNumber) count, sequence
            FROM seqofconn
            WHERE seqofconn.`group` IN (:groups)
            GROUP BY sequence
        ),
        TimeCodesOfSeq AS (
            SELECT seqofconn.sequence, timecode.*
            FROM timecode
            JOIN seqofconn ON seqofconn.line = timecode.line AND seqofconn.connNumber = timecode.connNumber
            JOIN counofconnsinsequence c ON c.sequence = seqofconn.sequence
            WHERE seqofconn.`group` IN (:groups)
            GROUP BY validFrom || validTo || type, seqofconn.sequence
            HAVING COUNT(DISTINCT seqofconn.connNumber) = c.count
            ORDER BY seqofconn.sequence, type, validTo, validFrom
        ),
        TimeCodesCountOfSeq AS (
            SELECT DISTINCT sequence, COUNT(*) count FROM TimeCodesOfSeq timecode
            GROUP BY sequence
        ),
        TodayRunningSequences AS (
            SELECT DISTINCT timecode.sequence FROM TimeCodesOfSeq timecode
            JOIN TimeCodesCountOfSeq ON TimeCodesCountOfSeq.sequence = timecode.sequence
            AND ((
                timecode.type = "RunsOnly"
                AND timecode.validFrom <= :date
                AND :date <= timecode.validTo
            ) OR (
                timecode.type = "RunsAlso"
            ) OR (
                timecode.type = "Runs"
                AND timecode.validFrom <= :date
                AND :date <= timecode.validTo
            ) OR (
                timecode.type = "DoesNotRun"
                AND NOT (
                    timecode.validFrom <= :date
                    AND :date <= timecode.validTo
                )
            ))
            GROUP BY timecode.sequence
            HAVING (
                timecode.runs2
                AND COUNT(*) >= 1
            ) OR (
                NOT timecode.runs2
                AND COUNT(*) = TimeCodesCountOfSeq.count
            )
        ),
        endStopIndexOnThisLine(sequence, time, name) AS (
            SELECT DISTINCT seqofconn.sequence, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END, stop.stopName FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN seqofconn ON seqofconn.connNumber = connstop.connNumber AND seqofconn.line = connstop.line
            WHERE (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND connstop.tab IN (:tabs)
            GROUP BY seqofconn.sequence
            HAVING MAX(CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END)
        ),
        startStopIndexOnThisLine(sequence, time, name) AS (
            SELECT DISTINCT seqofconn.sequence, CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END, stop.stopName FROM connstop
            JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
            JOIN seqofconn ON seqofconn.connNumber = connstop.connNumber AND seqofconn.line = connstop.line
            WHERE (
                NOT connstop.departure IS null
                OR NOT connstop.arrival IS null
            )
            AND connstop.tab IN (:tabs)
            GROUP BY seqofconn.sequence
            HAVING MIN(CASE
                WHEN connstop.departure IS null THEN connstop.arrival
                ELSE connstop.departure
            END)
        )
        SELECT seqofconn.sequence, conn.fixedCodes, startStopIndexOnThisLine.time start, endStopIndexOnThisLine.time `end`, timecode.type, timecode.validFrom `from`, timecode.validTo `to`, conn.name connName FROM conn
        JOIN startStopIndexOnThisLine ON startStopIndexOnThisLine.sequence = seqofconn.sequence
        JOIN endStopIndexOnThisLine ON endStopIndexOnThisLine.sequence = seqofconn.sequence
        JOIN timecode ON timecode.connNumber = conn.connNumber AND timecode.tab = conn.tab
        JOIN seqofconn ON seqofconn.connNumber = conn.connNumber AND seqofconn.line = conn.line
        WHERE seqofconn.sequence IN TodayRunningSequences
        AND conn.tab IN (:tabs)
        AND seqofconn.`group` IN (:groups)
    """ // DISTINCT zde není schválně - chceme pevné kódy pro každý spoj zvlášť, abychom poté získali společné pevné kódy
    )
    suspend fun fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
        date: LocalDate,
        tabs: List<Table>,
        groups: List<SequenceGroup>
    ): Map<TimeOfSequence, Map<@MapColumn("connName") BusName, List<CodesOfBus>>>

    @Query(
        """
        SELECT DISTINCT conn.line FROM conn
        GROUP BY conn.line
        HAVING COUNT(DISTINCT conn.direction) = 1
    """
    )
    suspend fun oneDirectionLines(): List<LongLine>

    @Transaction
    @Query(
        """
        SELECT conn.name connName, CASE
            WHEN connstop.departure IS null THEN connstop.arrival
            ELSE connstop.departure
        END time, stop.stopName name, connstop.stopIndexOnLine FROM conn
        JOIN connstop ON connstop.connNumber = conn.connNumber AND connstop.tab = conn.tab
        JOIN stop ON stop.stopNumber = connstop.stopNumber AND stop.tab = connstop.tab
        WHERE conn.name IN (:connNames)
        AND conn.tab IN (:tabs)
        ORDER BY CASE
           WHEN conn.direction = :positive THEN connstop.stopIndexOnLine
           ELSE -connstop.stopIndexOnLine
        END
    """
    )
    suspend fun connStops(connNames: List<BusName>, tabs: List<Table>, positive: Direction = Direction.POSITIVE): Map<@MapColumn("connName") BusName, List<StopOfDeparture>>

    @Query(
        """
        SELECT hasRestriction FROM line
        WHERE tab = :tab
        LIMIT 1
    """
    )
    suspend fun hasRestriction(tab: Table): Boolean

    @Query(
        """
        SELECT validFrom, validTo FROM line
        WHERE tab = :tab
        LIMIT 1
    """
    )
    suspend fun validity(tab: Table): Validity

    @Query(
        """
        SELECT name FROM conn
        WHERE name = :connName
        LIMIT 1
    """
    )
    suspend fun doesConnExist(connName: BusName): String?

    @Query(
        """
        SELECT * FROM line
        WHERE number = :line
    """
    )
    suspend fun lineTables(line: LongLine): List<Line>

    @Query(
        """
        SELECT DISTINCT seqgroup.* FROM seqgroup
        JOIN seqofconn ON seqofconn.`group` = seqgroup.`group`
        WHERE seqofconn.sequence = :seq
    """
    )
    suspend fun seqGroups(seq: SequenceCode): List<SeqGroup>

    @Query(
        """
        SELECT DISTINCT number FROM line
    """
    )
    suspend fun allLineNumbers(): List<LongLine>

    @Query(
        """
        SELECT DISTINCT sequence FROM seqofconn
    """
    )
    suspend fun allSequences(): List<SequenceCode>

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

    @Insert
    suspend fun insertSeqOfConns(vararg seqsOfBuses: SeqOfConn)

    @Insert
    suspend fun insertSeqGroups(vararg seqGroups: SeqGroup)

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

    @Query("SELECT * FROM seqofconn")
    suspend fun seqOfConns(): List<SeqOfConn>

    @Query("SELECT * FROM seqgroup")
    suspend fun seqGroups(): List<SeqGroup>
}