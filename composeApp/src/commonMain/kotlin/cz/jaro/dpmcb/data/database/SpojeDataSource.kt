package cz.jaro.dpmcb.data.database

import cz.jaro.dpmcb.data.entities.Conn
import cz.jaro.dpmcb.data.entities.ConnStop
import cz.jaro.dpmcb.data.entities.Line
import cz.jaro.dpmcb.data.entities.SeqGroup
import cz.jaro.dpmcb.data.entities.SeqOfConn
import cz.jaro.dpmcb.data.entities.Stop
import cz.jaro.dpmcb.data.entities.TimeCode

interface SpojeDataSource : SpojeQueries {
    suspend fun clearAllTables() {}

    val needsToDownloadData: Boolean

    fun insertConnStops(connStops: List<ConnStop>): List<suspend () -> Unit>
    fun insertStops(stops: List<Stop>): List<suspend () -> Unit>
    fun insertTimeCodes(timeCodes: List<TimeCode>): List<suspend () -> Unit>
    fun insertLines(lines: List<Line>): List<suspend () -> Unit>
    fun insertConns(conns: List<Conn>): List<suspend () -> Unit>
    fun insertSeqOfConns(seqsOfBuses: List<SeqOfConn>): List<suspend () -> Unit>
    fun insertSeqGroups(seqGroups: List<SeqGroup>): List<suspend () -> Unit>
}