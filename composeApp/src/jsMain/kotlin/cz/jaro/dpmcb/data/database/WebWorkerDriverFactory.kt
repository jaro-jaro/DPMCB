package cz.jaro.dpmcb.data.database

import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

object WebWorkerDriverFactory: DriverFactory {
    override val driver = WebWorkerDriver(
        Worker(
            js("""new URL("sqljs.worker.js", import.meta.url)""")
        )
    )
}