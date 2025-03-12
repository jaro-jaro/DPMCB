package cz.jaro.dpmcb.data.database

interface SpojeDataSource : SpojeQueries {
    fun clearAllTables()
}