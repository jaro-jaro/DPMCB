package cz.jaro.dpmcb.data

import android.util.Log
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

actual fun recordException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}

actual fun logToCrashlytics(message: Any?) {
    Firebase.crashlytics.log(message.toString())
}

actual fun log(tag: String?, message: String) {
    Log.d(tag, message)
}