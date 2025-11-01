package cz.jaro.dpmcb.data

expect fun recordException(throwable: Throwable)
expect fun logToCrashlytics(message: Any?)
expect fun log(tag: String?, message: String)