package cz.jaro.dpmcb.data.database

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import cz.jaro.dpmcb.Database

class AndroidDriverFactory(context: Context) : DriverFactory {
    override val driver =
        AndroidSqliteDriver(Database.Schema.synchronous(), context, "dpmcb-jaro.db")
}