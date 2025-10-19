package cz.jaro.dpmcb.data

actual fun log(message: Any?) {
    console.log(message)
}

actual fun recordException(throwable: Throwable) {
    console.error(throwable.stackTraceToString())
}