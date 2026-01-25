package cz.jaro.dpmcb.data

class JsLogger(
    debugManager: DebugManager,
) : Logger, DebugManager by debugManager {
    override fun logToCrashlytics(message: Any?) {
        console.log(message)
    }

    override fun recordException(throwable: Throwable) {
        console.error(throwable.stackTraceToString())
    }

    override fun log(tag: String?, message: String) {
        if (tag != null) console.log(tag, message) else console.log(message)
    }
}
