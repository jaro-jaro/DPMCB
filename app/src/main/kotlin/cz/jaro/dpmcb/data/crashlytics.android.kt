package cz.jaro.dpmcb.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

fun recordException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}

fun log(message: String) {
    Firebase.crashlytics.log(message)
}