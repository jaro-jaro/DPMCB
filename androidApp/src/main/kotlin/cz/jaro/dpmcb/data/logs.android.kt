package cz.jaro.dpmcb.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

class AndroidLogger(
    debugManager: DebugManager,
) : Logger, DebugManager by debugManager {
    override fun recordException(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }

    override fun logToCrashlytics(message: Any?) {
        Firebase.crashlytics::log.invoke(message.toString())
    }

    override fun log(tag: String?, message: String) {
        Log.d(tag, message)
    }
}