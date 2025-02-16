package cz.jaro.dpmcb.ui.bus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch

class BroadcastReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type")
        if (type != null) scope.launch {
            clicked.send(type)
        }
    }
    companion object {
        val clicked = Channel<String>(UNLIMITED)

        fun createIntent(context: Context, type: String) =
            Intent(context, BroadcastReceiver::class.java).apply {
                putExtra("type", type)
            }

        const val TYPE_COPY = "copy"
        const val TYPE_ADD_IMAGE = "add_image"
        const val TYPE_SHARE_PART = "share_part"
    }
}
