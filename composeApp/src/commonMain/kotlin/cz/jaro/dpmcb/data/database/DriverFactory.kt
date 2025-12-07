package cz.jaro.dpmcb.data.database

import app.cash.sqldelight.db.SqlDriver

interface DriverFactory {
    val driver: SqlDriver
}