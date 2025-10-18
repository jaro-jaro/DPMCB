package cz.jaro.dpmcb.ui.bus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class BroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type")?.let(ActionType::valueOf)
        if (type != null) scope.launch {
            send(type)
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)

        private val clicked = Channel<ActionType>(UNLIMITED)
        suspend fun send(type: ActionType) = clicked.send(type)
        val clickedFlow = clicked.consumeAsFlow().shareIn(scope, SharingStarted.Eagerly)

        fun createIntent(context: Context, type: ActionType) =
            Intent(context, cz.jaro.dpmcb.ui.bus.BroadcastReceiver::class.java).apply {
                putExtra("type", type.name)
            }
    }

    enum class ActionType {
        COPY,
        ADD_IMAGE,
        SHARE_PART,
        REMOVE_DATE_FROM_BUS,
        REMOVE_DATE,
        REMOVE_DATE_FROM_IMAGE,
        REMOVE_DATE_FROM_IMAGE_PART,
    }
}
