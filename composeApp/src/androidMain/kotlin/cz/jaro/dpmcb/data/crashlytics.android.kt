package cz.jaro.dpmcb.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

actual fun recordException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}

actual fun log(message: Any?) {
    Firebase.crashlytics.log(message.toString())
}