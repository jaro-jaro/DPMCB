package cz.jaro.dpmcb.data

actual fun logToCrashlytics(message: Any?) {
    console.log(message)
}

actual fun recordException(throwable: Throwable) {
    console.error(throwable.stackTraceToString())
}

actual fun log(tag: String?, message: String) {
    if (tag != null) console.log(tag, message) else console.log(message)
}