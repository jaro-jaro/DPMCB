package cz.jaro.dpmcb.ui.favourites.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import cz.jaro.dpmcb.LoadingActivity
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.SpojeRepository
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.allTrue
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.plus
import cz.jaro.dpmcb.data.helperclasses.timeHere
import cz.jaro.dpmcb.data.helperclasses.todayHere
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes


class OblibeneWidget : GlanceAppWidget() {
    companion object {

        val PREFS_KEY_DATA = stringPreferencesKey("data")
        const val EXTRA_KEY_WIDGET_IDS = "providerwidgetids"

        class Reciever : GlanceAppWidgetReceiver(), KoinComponent {
            override val glanceAppWidget: GlanceAppWidget = OblibeneWidget()

            val repo by inject<SpojeRepository>()

            override fun onReceive(context: Context, intent: Intent) {
                if (intent.hasExtra(EXTRA_KEY_WIDGET_IDS)) {
                    val ids = intent.extras!!.getIntArray(EXTRA_KEY_WIDGET_IDS)
                    onUpdate(context, AppWidgetManager.getInstance(context), ids!!)
                } else super.onReceive(context, intent)
            }

            override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
                super.onUpdate(context, appWidgetManager, appWidgetIds)

                CoroutineScope(Dispatchers.IO).launch {

                    val state = suspend state@{
                        val ids =
                            repo.favourites.first().map { cast ->
                                try {
                                    val a = repo.favouriteBus(cast.busName, SystemClock.todayHere())
                                    Triple(a.first, a.second, cast)
                                } catch (e: Exception) {
                                    Firebase.crashlytics.recordException(e)
                                    return@state OblibeneWidgetState.Error
                                }
                            }
                        val date = repo.date.first()

                        if (ids.isEmpty()) return@state OblibeneWidgetState.ZadneOblibene

                        val tedJede = ids.map { (info, zastavky, cast) ->
                            val jedeV = repo.doesConnRunAt(info.connName)
                            KartickaWidgetState(
                                spojId = info.connName,
                                linka = info.line,
                                vychoziZastavka = (zastavky.getOrNull(cast.start) ?: zastavky.last()).name,
                                vychoziZastavkaCas = (zastavky.getOrNull(cast.start) ?: zastavky.last()).time,
                                cilovaZastavka = (zastavky.getOrNull(cast.end) ?: zastavky.last()).name,
                                cilovaZastavkaCas = (zastavky.getOrNull(cast.end) ?: zastavky.last()).time,
                            ) to listOf(
                                jedeV(SystemClock.todayHere()),
                                zastavky[cast.start].time <= SystemClock.timeHere().plus(30.minutes, date),
                                SystemClock.timeHere() <= zastavky[cast.end].time.plus(30.minutes, date),
                            ).allTrue()
                        }
                            .filter { it.second }.map { it.first }
                            .sortedBy { it.vychoziZastavkaCas }

                        if (tedJede.isEmpty()) return@state OblibeneWidgetState.PraveNicNejede

                        OblibeneWidgetState.TedJede(tedJede)
                    }()

                    appWidgetIds.forEach {
                        val id = GlanceAppWidgetManager(context).getGlanceIdBy(it)

                        updateAppWidgetState(context, id) { prefs ->
                            prefs[PREFS_KEY_DATA] = Json.encodeToString(state)
                        }
                        glanceAppWidget.update(context, id)
                    }
                }
            }

        }

        fun updateAll(context: Context) {
            context.sendBroadcast(Intent().apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

                val appWidgetManager = AppWidgetManager.getInstance(context)

                putExtra(
                    EXTRA_KEY_WIDGET_IDS, appWidgetManager.getAppWidgetIds(
                        ComponentName(context, Reciever::class.java)
                    )
                )
            })
        }
    }

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val state = prefs[PREFS_KEY_DATA]?.let { Json.decodeFromString<OblibeneWidgetState>(it) } ?: OblibeneWidgetState.NacitaSe

                val style = TextStyle(ColorProvider(R.color.on_background_color))
                val action = actionStartActivity<LoadingActivity>()

                @Composable
                fun Refresh(text: String) {
                    Row(
                        GlanceModifier.fillMaxWidth()
                    ) {
                        Text(text, GlanceModifier.clickable(action).defaultWeight(), style = style)
                        Image(
                            provider = ImageProvider(R.drawable.baseline_refresh_24),
                            colorFilter = ColorFilter.tint(ColorProvider(R.color.on_background_color)),
                            contentDescription = "Aktualizovat",
                            modifier = GlanceModifier.clickable {
                                updateAll(context)
                            },
                        )
                    }
                }

                Box(
                    GlanceModifier.background(R.color.background_color).padding(all = 8.dp).fillMaxSize(),
                ) {
                    when (state) {
                        OblibeneWidgetState.Error -> Refresh("Něco se nepovedlo :(")
                        OblibeneWidgetState.NacitaSe -> Refresh("Načítání...")
                        OblibeneWidgetState.ZadneOblibene -> Refresh("Zatím nemáte žádné oblíbené spoje. Přidejte si je kliknutím na ikonu hvězdičky v detailu spoje")
                        OblibeneWidgetState.PraveNicNejede -> Refresh("Právě nejede žádný z vašich oblíbených spojů")
                        is OblibeneWidgetState.TedJede -> LazyColumn(
                            GlanceModifier.fillMaxSize(),
                        ) {
                            item {
                                Refresh("Oblíbené spoje")
                            }
                            items(state.spoje) {
                                Spoj(state = it, style = style, context = context)
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun Spoj(
        state: KartickaWidgetState,
        style: TextStyle,
        context: Context
    ) = Column(
        GlanceModifier.fillMaxWidth(),
    ) {
        val action = actionStartActivity(Intent(context, LoadingActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "https://jaro-jaro.github.io/DPMCB/bus/${state.spojId}".toUri()
        })

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(action),
        ) {
            Text(text = "${state.linka}", GlanceModifier.clickable(action), style = style)
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(action),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(text = state.vychoziZastavka, GlanceModifier.clickable(action).defaultWeight(), style = style)
            Text(text = state.vychoziZastavkaCas.toString(), GlanceModifier.clickable(action), style = style)
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(action),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(text = state.cilovaZastavka, GlanceModifier.clickable(action).defaultWeight(), style = style)
            Text(text = "${state.cilovaZastavkaCas}", GlanceModifier.clickable(action), style = style)
        }
    }
}
