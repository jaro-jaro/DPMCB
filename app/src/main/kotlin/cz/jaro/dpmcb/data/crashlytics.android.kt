package cz.jaro.dpmcb.data

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

fun recordException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}

fun log(message: String) {
    Firebase.crashlytics.log(message)
}