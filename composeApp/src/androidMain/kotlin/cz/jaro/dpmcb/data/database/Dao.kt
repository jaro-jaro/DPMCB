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
import cz.jaro.dpmcb.data.realtions.timetable.EndStop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
interface Dao : SpojeQueries {
    @Transaction
    @Query(
        """
        SELECT (Conn.fixedCodes LIKE '%{%') lowFloor, Conn.line, Conn.fixedCodes, Conn.direction, Line.vehicleType, CASE
            WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
            ELSE ConnStop.departure
        END time, ConnStop.arrival, Stop.fixedCodes stopFixedCodes, ConnStop.fixedCodes connStopFixedCodes, stopName name, SeqOfConn.sequence, Conn.name connName, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to`, SeqOfConn.`group` FROM ConnStop
        JOIN Conn ON Conn.tab = ConnStop.tab AND Conn.connNumber = ConnStop.connNumber
        JOIN Stop ON Stop.tab = ConnStop.tab AND Stop.stopNumber = ConnStop.stopNumber
        JOIN Line ON Line.tab = ConnStop.tab
        JOIN TimeCode ON TimeCode.tab = ConnStop.tab AND TimeCode.connNumber = ConnStop.connNumber
        JOIN SeqOfConn ON SeqOfConn.line = Conn.line AND SeqOfConn.connNumber = Conn.connNumber
        WHERE (
            ConnStop.departure IS NOT NULL
            OR ConnStop.arrival IS NOT NULL
        )
        AND Conn.name = :connName
        AND Conn.tab = :tab
        AND SeqOfConn.`group` IN (:groups)
        ORDER BY CASE
           WHEN Conn.direction = 'POSITIVE' THEN ConnStop.stopIndexOnLine
           ELSE -ConnStop.stopIndexOnLine
        END;
        """
    )
    override suspend fun coreBus(connName: BusName, groups: List<SequenceGroup>, tab: Table): List<CoreBus>

    @Query(
        """
        SELECT Conn.fixedCodes, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to` FROM TimeCode
        JOIN Conn ON Conn.tab = TimeCode.tab AND Conn.connNumber = TimeCode.connNumber
        AND Conn.name = :connName
        AND Conn.tab = :tab;
        """
    )
    override suspend fun codes(connName: BusName, tab: Table): List<CodesOfBus>

    @Transaction
    @Query(
        """
        SELECT (Conn.fixedCodes LIKE '%{%') lowFloor, Conn.line, Conn.direction, SeqOfConn.sequence, Line.vehicleType, Conn.fixedCodes, CASE
            WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
            ELSE ConnStop.departure
        END time, ConnStop.arrival, Stop.fixedCodes stopFixedCodes, Line.validFrom, Line.validTo, ConnStop.fixedCodes connStopFixedCodes, stopName name, Conn.name connName, Conn.tab, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to` FROM ConnStop
        JOIN Conn ON Conn.tab = ConnStop.tab AND Conn.connNumber = ConnStop.connNumber
        JOIN Stop ON Stop.tab = ConnStop.tab AND Stop.stopNumber = ConnStop.stopNumber
        JOIN Line ON Line.tab = Conn.tab AND Line.number = Conn.line
        JOIN TimeCode ON TimeCode.tab = ConnStop.tab AND TimeCode.connNumber = ConnStop.connNumber
        JOIN SeqOfConn ON SeqOfConn.line = Conn.line AND SeqOfConn.connNumber = Conn.connNumber
        WHERE (
            ConnStop.departure IS NOT NULL
            OR ConnStop.arrival IS NOT NULL
        )
        AND SeqOfConn.`group` = :group
        AND SeqOfConn.sequence LIKE :seq
        ORDER BY CASE
           WHEN Conn.direction = 'POSITIVE' THEN ConnStop.stopIndexOnLine
           ELSE -ConnStop.stopIndexOnLine
        END;
        """
    )
    override suspend fun coreBusOfSequence(seq: SequenceCode, group: SequenceGroup?): List<CoreBusOfSequence>

    @Transaction
    @Query(
        """
        SELECT DISTINCT Conn.name connName FROM Conn
        JOIN SeqOfConn ON SeqOfConn.connNumber = Conn.connNumber AND SeqOfConn.line = Conn.line
        WHERE SeqOfConn.sequence = :seq
        AND SeqOfConn.`group` = :group
        AND Conn.tab IN (:tabs)
        ORDER BY SeqOfConn.orderInSequence;
        """
    )
    override suspend fun connsOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): List<BusName>

    @Transaction
    @Query(
        """
        SELECT DISTINCT Conn.name connName FROM Conn
        JOIN SeqOfConn ON SeqOfConn.connNumber = Conn.connNumber AND SeqOfConn.line = Conn.line
        WHERE SeqOfConn.sequence = :seq
        AND SeqOfConn.`group` = :group
        AND Conn.tab IN (:tabs)
        ORDER BY SeqOfConn.orderInSequence
        LIMIT 1;
        """
    )
    override suspend fun firstConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName

    @Transaction
    @Query(
        """
        SELECT DISTINCT Conn.name connName FROM Conn
        JOIN SeqOfConn ON SeqOfConn.connNumber = Conn.connNumber AND SeqOfConn.line = Conn.line
        WHERE SeqOfConn.sequence = :seq
        AND SeqOfConn.`group` = :group
        AND Conn.tab IN (:tabs)
        ORDER BY -SeqOfConn.orderInSequence
        LIMIT 1;
        """
    )
    override suspend fun lastConnOfSeq(seq: SequenceCode, group: SequenceGroup?, tabs: List<Table>): BusName

    @Query(
        """
        WITH hereRunningConns AS (
            SELECT DISTINCT Conn.* FROM ConnStop
            JOIN Conn ON Conn.tab = ConnStop.tab AND Conn.connNumber = ConnStop.connNumber
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            WHERE Stop.stopName = :stop
            AND Conn.tab IN (:tabs)
            AND (
                ConnStop.departure IS NOT NULL
                OR ConnStop.arrival IS NOT NULL
            )
        ),
        thisStopIndexes AS (
            SELECT DISTINCT ConnStop.stopIndexOnLine, ConnStop.tab FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab =  ConnStop.tab
            AND Stop.stopName = :stop
            AND Conn.tab IN (:tabs)
            ORDER BY ConnStop.stopIndexOnLine
        ),
        thisStop AS (
            SELECT Stop.stopName name, Conn.fixedCodes, Conn.direction, CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END time, ConnStop.stopIndexOnLine, ConnStop.fixedCodes connStopFixedCodes, Conn.connNumber, Conn.line, Conn.tab, (Conn.fixedCodes LIKE '%{%') lowFloor
            FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN hereRunningConns Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
            CROSS JOIN thisStopIndexes thisStop ON ConnStop.tab = thisStop.tab AND ConnStop.stopIndexOnLine = thisStop.stopIndexOnLine
        )
        SELECT thisStop.*, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to`
        FROM thisStop
        JOIN hereRunningConns Conn ON Conn.connNumber = thisStop.connNumber AND Conn.tab = thisStop.tab
        JOIN TimeCode ON TimeCode.connNumber = thisStop.connNumber AND TimeCode.tab = thisStop.tab;
        """
    )
    override suspend fun departures(
        stop: String,
        tabs: List<Table>,
    ): List<CoreDeparture>

    @Query(
        """
        SELECT DISTINCT SeqOfConn.sequence FROM SeqOfConn
        WHERE 0
        OR SeqOfConn.sequence LIKE :sequence1
        OR SeqOfConn.sequence LIKE :sequence2
        OR SeqOfConn.sequence LIKE :sequence3
        OR SeqOfConn.sequence LIKE :sequence4
        OR SeqOfConn.sequence LIKE :sequence5
        OR SeqOfConn.sequence LIKE :sequence6;
        """
    )
    override suspend fun findSequences(
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
        WITH lastStopTimeOfConn(connName, time, tab) AS (
            SELECT DISTINCT Conn.name, CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END, ConnStop.tab FROM ConnStop
            JOIN Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
            WHERE (
                ConnStop.departure IS NOT NULL
                OR ConnStop.arrival IS NOT NULL
            )
            AND ConnStop.tab IN (:tabs)
            GROUP BY Conn.name
            HAVING MAX(CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END)
        )
        SELECT DISTINCT SeqOfConn.sequence, lastStopTimeOfConn.connName, lastStopTimeOfConn.time FROM Conn
        JOIN SeqOfConn ON SeqOfConn.connNumber = Conn.connNumber AND SeqOfConn.line = Conn.line
        JOIN lastStopTimeOfConn ON lastStopTimeOfConn.connName = Conn.name AND lastStopTimeOfConn.tab = Conn.tab
        WHERE SeqOfConn.sequence IN (:todayRunningSequences)
        AND SeqOfConn.`group` IN (:groups)
        AND Conn.tab IN (:tabs)
        ORDER BY SeqOfConn.sequence, lastStopTimeOfConn.time;
        """
    )
    override suspend fun lastStopTimesOfConnsInSequences(
        todayRunningSequences: List<SequenceCode>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<@MapColumn("sequence") SequenceCode, Map<@MapColumn("connName") BusName, @MapColumn("time") LocalTime>>

    @Query(
        """
        
        SELECT DISTINCT Stop.stopName FROM ConnStop
        JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
        JOIN Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
        WHERE ConnStop.line = :line
        AND Conn.tab = :tab
        ORDER BY ConnStop.stopIndexOnLine;
        """
    )
    override suspend fun stopNamesOfLine(line: LongLine, tab: Table): List<String>

    @Transaction
    @Query(
        """
        SELECT Conn.name connName, Conn.line, Conn.direction, Conn.tab, CASE
            WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
            ELSE ConnStop.departure
        END time, Stop.stopName name, SeqOfConn.sequence FROM Conn
        JOIN ConnStop ON ConnStop.connNumber = Conn.connNumber AND ConnStop.tab = Conn.tab
        JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
        JOIN SeqOfConn ON SeqOfConn.connNumber = Conn.connNumber AND SeqOfConn.line = Conn.line
        WHERE Conn.name IN (:connNames)
        AND SeqOfConn.`group` IN (:groups)
        AND Conn.tab IN (:tabs)
        AND (
            ConnStop.departure IS NOT NULL
            OR ConnStop.arrival IS NOT NULL
        )
        ORDER BY CASE
           WHEN Conn.direction = 'POSITIVE' THEN ConnStop.stopIndexOnLine
           ELSE -ConnStop.stopIndexOnLine
        END;
        """
    )
    override suspend fun nowRunningBuses(
        connNames: List<BusName>,
        groups: List<SequenceGroup>,
        tabs: List<Table>,
    ): Map<BusOfNowRunning, List<StopOfNowRunning>>

    @Transaction
    @Query(
        """
        WITH CountOfConnsInSequence AS (
            SELECT COUNT(DISTINCT connNumber) count, sequence
            FROM SeqOfConn
            WHERE SeqOfConn.`group` IN (:groups)
            GROUP BY sequence
        ),
        TimeCodesOfSeq AS (
            SELECT SeqOfConn.sequence, TimeCode.*
            FROM TimeCode
            JOIN SeqOfConn ON SeqOfConn.line = TimeCode.line AND SeqOfConn.connNumber = TimeCode.connNumber
            JOIN CountOfConnsInSequence c ON c.sequence = SeqOfConn.sequence
            WHERE SeqOfConn.`group` IN (:groups)
            GROUP BY validFrom || validTo || type, SeqOfConn.sequence, TimeCode.tab
            HAVING COUNT(DISTINCT SeqOfConn.connNumber) = c.count
            ORDER BY SeqOfConn.sequence, type, validTo, validFrom
        ),
        TimeCodesCountOfSeq AS (
            SELECT DISTINCT sequence, COUNT(*) count FROM TimeCodesOfSeq TimeCode
            GROUP BY sequence, tab
        ),
        TodayRunningSequences AS (
            SELECT DISTINCT TimeCode.sequence FROM TimeCodesOfSeq TimeCode
            JOIN TimeCodesCountOfSeq ON TimeCodesCountOfSeq.sequence = TimeCode.sequence
            AND ((
                TimeCode.type = 'RunsOnly'
                AND TimeCode.validFrom <= :date
                AND :date <= TimeCode.validTo
            ) OR (
                TimeCode.type = 'RunsAlso'
            ) OR (
                TimeCode.type = 'Runs'
                AND TimeCode.validFrom <= :date
                AND :date <= TimeCode.validTo
            ) OR (
                TimeCode.type = 'DoesNotRun'
                AND NOT (
                    TimeCode.validFrom <= :date
                    AND :date <= TimeCode.validTo
                )
            ))
            GROUP BY TimeCode.sequence, TimeCode.tab
            HAVING (
                TimeCode.runs2
                AND COUNT(*) >= 1
            ) OR (
                NOT TimeCode.runs2
                AND COUNT(*) = TimeCodesCountOfSeq.count
            )
        ),
        endStopIndexOnThisLine(sequence, time, name) AS (
            SELECT DISTINCT SeqOfConn.sequence, CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END, Stop.stopName FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN SeqOfConn ON SeqOfConn.connNumber = ConnStop.connNumber AND SeqOfConn.line = ConnStop.line
            WHERE (
                ConnStop.departure IS NOT NULL
                OR ConnStop.arrival IS NOT NULL
            )
            AND ConnStop.tab IN (:tabs)
            GROUP BY SeqOfConn.sequence
            HAVING MAX(CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END)
        ),
        startStopIndexOnThisLine(sequence, time, name) AS (
            SELECT DISTINCT SeqOfConn.sequence, CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END, Stop.stopName FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN SeqOfConn ON SeqOfConn.connNumber = ConnStop.connNumber AND SeqOfConn.line = ConnStop.line
            WHERE (
                ConnStop.departure IS NOT NULL
                OR ConnStop.arrival IS NOT NULL
            )
            AND ConnStop.tab IN (:tabs)
            GROUP BY SeqOfConn.sequence
            HAVING MIN(CASE
                WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
                ELSE ConnStop.departure
            END)
        )
        SELECT SeqOfConn.sequence, Conn.fixedCodes, startStopIndexOnThisLine.time start, endStopIndexOnThisLine.time `end`, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to`, Conn.name connName FROM Conn
        JOIN SeqOfConn ON SeqOfConn.connNumber = Conn.connNumber AND SeqOfConn.line = Conn.line
        JOIN startStopIndexOnThisLine ON startStopIndexOnThisLine.sequence = SeqOfConn.sequence
        JOIN endStopIndexOnThisLine ON endStopIndexOnThisLine.sequence = SeqOfConn.sequence
        JOIN TimeCode ON TimeCode.connNumber = Conn.connNumber AND TimeCode.tab = Conn.tab
        WHERE SeqOfConn.sequence IN TodayRunningSequences
        AND Conn.tab IN (:tabs)
        AND SeqOfConn.`group` IN (:groups);
        """
    )
    override suspend fun fixedCodesOfTodayRunningSequencesAccordingToTimeCodes(
        date: LocalDate,
        tabs: List<Table>,
        groups: List<SequenceGroup>,
    ): Map<TimeOfSequence, Map<@MapColumn("connName") BusName, List<CodesOfBus>>>

    @Query(
        """
        SELECT DISTINCT Conn.line FROM Conn
        GROUP BY Conn.line
        HAVING COUNT(DISTINCT Conn.direction) = 1;
        """
    )
    override suspend fun oneDirectionLines(): List<LongLine>

    @Transaction
    @Query(
        """
        SELECT Conn.name connName, CASE
            WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
            ELSE ConnStop.departure
        END time, Stop.stopName name, ConnStop.stopIndexOnLine FROM Conn
        JOIN ConnStop ON ConnStop.connNumber = Conn.connNumber AND ConnStop.tab = Conn.tab
        JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
        WHERE (
            ConnStop.departure IS NOT NULL
            OR ConnStop.arrival IS NOT NULL
        )
        AND Conn.name IN (:connNames)
        AND Conn.tab IN (:tabs)
        ORDER BY CASE
           WHEN Conn.direction = 'POSITIVE' THEN ConnStop.stopIndexOnLine
           ELSE -ConnStop.stopIndexOnLine
        END;
        """
    )
    override suspend fun connStops(connNames: List<BusName>, tabs: List<Table>): Map<@MapColumn("connName") BusName, List<StopOfDeparture>>

    @Query(
        """
        SELECT hasRestriction FROM Line
        WHERE tab = :tab
        LIMIT 1;
        """
    )
    override suspend fun hasRestriction(tab: Table): Boolean

    @Query(
        """
        SELECT validFrom, validTo FROM Line
        WHERE tab = :tab
        LIMIT 1;
        """
    )
    override suspend fun validity(tab: Table): Validity

    @Query(
        """
        SELECT name FROM Conn
        WHERE name = :connName
        LIMIT 1;
        """
    )
    override suspend fun doesConnExist(connName: BusName): String?

    @Query(
        """
        SELECT DISTINCT SeqGroup.*, SeqOfConn.`sequence` FROM SeqGroup
        JOIN SeqOfConn ON SeqOfConn.`group` = SeqGroup.`group`
        GROUP BY SeqOfConn.`sequence`;
        """
    )
    override suspend fun seqGroupsPerSequence(): Map<@MapColumn("sequence") SequenceCode, List<SeqGroup>>

    @Query(
        """
        SELECT DISTINCT number FROM Line;
        """
    )
    override suspend fun allLineNumbers(): List<LongLine>

    @Query(
        """
        SELECT DISTINCT sequence FROM SeqOfConn;
        """
    )
    override suspend fun allSequences(): List<SequenceCode>

    @Insert
    suspend fun insertConnStops2(connStops: List<ConnStop>)

    @Insert
    suspend fun insertStops2(stops: List<Stop>)

    @Insert
    suspend fun insertTimeCodes2(timeCodes: List<TimeCode>)

    @Insert
    suspend fun insertLines2(lines: List<Line>)

    @Insert
    suspend fun insertConns2(conns: List<Conn>)

    @Insert
    suspend fun insertSeqOfConns2(seqsOfBuses: List<SeqOfConn>)

    @Insert
    suspend fun insertSeqGroups2(seqGroups: List<SeqGroup>)

    @Query("SELECT * FROM connstop")
    override suspend fun connStops(): List<ConnStop>

    @Query("SELECT * FROM stop")
    override suspend fun stops(): List<Stop>

    @Query("SELECT * FROM timecode")
    override suspend fun timeCodes(): List<TimeCode>

    @Query("SELECT * FROM line")
    override suspend fun lines(): List<Line>

    @Query("SELECT * FROM conn")
    override suspend fun conns(): List<Conn>

    @Query("SELECT * FROM seqofconn")
    override suspend fun seqOfConns(): List<SeqOfConn>

    @Query("SELECT * FROM seqgroup")
    override suspend fun seqGroups(): List<SeqGroup>

    @Query(
        """
        WITH HereRunningConns AS (
            SELECT DISTINCT Conn.* FROM ConnStop
            JOIN Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            WHERE Conn.tab = :tab
            AND Stop.stopName = :stop
            AND Conn.direction = :direction
            AND (
                ConnStop.departure IS NOT NULL
                OR ConnStop.arrival IS NOT NULL
            )
        )
        SELECT CASE
            WHEN ConnStop.departure IS NULL THEN ConnStop.arrival
            ELSE ConnStop.departure
        END time, Conn.name connName, Stop.stopName, Conn.fixedCodes, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to` FROM TimeCode
        JOIN HereRunningConns Conn ON Conn.connNumber = TimeCode.connNumber AND Conn.tab = TimeCode.tab
        JOIN ConnStop ON ConnStop.connNumber = TimeCode.connNumber AND ConnStop.tab = TimeCode.tab
        JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
        ORDER BY CASE
            WHEN Conn.direction = 'NEGATIVE' THEN -ConnStop.stopIndexOnLine
            ELSE ConnStop.stopIndexOnLine
        END
        """
    )
    override suspend fun connStopsOnLineWithNextStopAtDate(
        stop: String,
        direction: Direction,
        tab: Table,
    ): List<CoreBusInTimetable>
    @Query(
        """
        WITH HereRunningConns AS (
            SELECT DISTINCT Conn.* FROM ConnStop
            JOIN Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            WHERE Conn.tab = :tab
            AND Stop.stopName = :stop
            AND (
                ConnStop.departure IS NOT NULL
                OR ConnStop.arrival IS NOT NULL
            )
        )
        SELECT Conn.direction, ConnStop.stopIndexOnLine, Conn.name connName, Stop.stopName, Conn.fixedCodes, TimeCode.type, TimeCode.validFrom `from`, TimeCode.validTo `to` FROM TimeCode
        JOIN HereRunningConns Conn ON Conn.connNumber = TimeCode.connNumber AND Conn.tab = TimeCode.tab
        JOIN ConnStop ON ConnStop.connNumber = TimeCode.connNumber AND ConnStop.tab = TimeCode.tab
        JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
        ORDER BY CASE
            WHEN Conn.direction = 'NEGATIVE' THEN -ConnStop.stopIndexOnLine
            ELSE ConnStop.stopIndexOnLine
        END
        """
    )
    override suspend fun endStops(
        stop: String,
        tab: Table,
    ): List<EndStop>

    @Query(
        """
        SELECT number FROM Line
        WHERE shortNumber = :line
        LIMIT 1;
        """
    )
    override suspend fun findLongLine(line: ShortLine): LongLine

    @Query(
        """
        SELECT DISTINCT shortNumber FROM Line
        WHERE tab IN (:tabs)
        ORDER BY shortNumber;
        """
    )
    override suspend fun lineNumbers(tabs: List<Table>): List<ShortLine>

    @Query(
        """
        SELECT DISTINCT stopName FROM Stop
        WHERE tab IN (:tabs)
        ORDER BY stopName;
        """
    )
    override suspend fun stopNames(tabs: List<Table>): List<String>

    @Query(
        """
        WITH hereRunningConns AS (
            SELECT DISTINCT Conn.* FROM ConnStop
            JOIN Conn ON Conn.tab = ConnStop.tab AND Conn.connNumber = ConnStop.connNumber
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            WHERE Stop.stopName = :thisStop
            AND ConnStop.line = :line
            AND ConnStop.departure IS NOT NULL
        ),
        indexesOfThisStop AS (
            SELECT DISTINCT ConnStop.stopIndexOnLine, departure, ConnStop.connNumber, ConnStop.tab FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab =  ConnStop.tab
            WHERE ConnStop.line = :line
            AND Stop.stopName = :thisStop
            AND ConnStop.departure IS NOT NULL
            ORDER BY ConnStop.stopIndexOnLine
        ),
        negative(max, stopName, indexOnLine, Line, connNumber) AS (
            SELECT DISTINCT MAX(ConnStop.stopIndexOnLine) AS max, Stop.stopName, ConnStop.stopIndexOnLine, :line, Conn.connNumber
            FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN hereRunningConns Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
            JOIN indexesOfThisStop thisStop ON thisStop.connNumber = ConnStop.connNumber AND thisStop.tab = ConnStop.tab
            WHERE ConnStop.stopIndexOnLine < thisStop.stopIndexOnLine
            AND Conn.tab = :tab
            AND Conn.direction <> 'POSITIVE'
            AND ConnStop.arrival IS NOT NULL
            AND thisStop.departure IS NOT NULL
            GROUP BY Conn.connNumber, thisStop.stopIndexOnLine
            ORDER BY -ConnStop.stopIndexOnLine
        ),
        positive(min, stopName, indexOnLine, Line, connNumber) AS (
            SELECT DISTINCT MIN(ConnStop.stopIndexOnLine), Stop.stopName, ConnStop.stopIndexOnLine, :line, Conn.connNumber
            FROM ConnStop
            JOIN Stop ON Stop.stopNumber = ConnStop.stopNumber AND Stop.tab = ConnStop.tab
            JOIN hereRunningConns Conn ON Conn.connNumber = ConnStop.connNumber AND Conn.tab = ConnStop.tab
            JOIN indexesOfThisStop thisStop ON thisStop.connNumber = ConnStop.connNumber AND thisStop.tab = ConnStop.tab
            WHERE ConnStop.stopIndexOnLine > thisStop.stopIndexOnLine
            AND Conn.tab = :tab
            AND Conn.direction = 'POSITIVE'
            AND ConnStop.arrival IS NOT NULL
            AND thisStop.departure IS NOT NULL
            GROUP BY Conn.connNumber, thisStop.stopIndexOnLine
            ORDER BY ConnStop.stopIndexOnLine
        )
        SELECT DISTINCT stopName
        FROM positive
        UNION
        SELECT DISTINCT stopName
        FROM negative;
        """
    )
    override suspend fun nextStops(line: LongLine, thisStop: String, tab: Table): List<String>

}
