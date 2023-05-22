package cz.jaro.dpmcb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle


class ExitActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finishAndRemoveTask()
    }

    companion object {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

        fun exitApplication(context: Context) {
            context.startActivity(Intent(context, ExitActivity::class.java).apply {
                addFlags(this@Companion.flags)
            })
        }
    }
}