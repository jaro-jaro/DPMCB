package cz.jaro.dpmcb.data.database

interface SpojeDataSource : SpojeQueries {
    suspend fun clearAllTables() {}

    val needsToDownloadData: Boolean
}