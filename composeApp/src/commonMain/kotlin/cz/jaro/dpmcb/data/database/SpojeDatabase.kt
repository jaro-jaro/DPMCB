package cz.jaro.dpmcb.data.database

interface SpojeDatabase {
    fun dataSource(): DataSource

    fun clearAllTables()
}