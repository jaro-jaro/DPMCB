package cz.jaro.dpmcb.data.database

interface SpojeDataSource {
    suspend fun dropAllTables()
    suspend fun createTables()

    val needsToDownloadData: Boolean

    val q : SpojeQueries
}