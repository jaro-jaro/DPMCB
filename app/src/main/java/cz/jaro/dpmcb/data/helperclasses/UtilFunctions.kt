package cz.jaro.dpmcb.data.helperclasses

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction
import cz.jaro.dpmcb.BuildConfig
import cz.jaro.dpmcb.data.nastaveni
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.sign
import kotlin.time.toJavaDuration

object UtilFunctions {

    fun LocalDate.hezky() = LocalDate.now().until(this, ChronoUnit.DAYS).let { za ->
        when (za) {
            0L -> "dnes"
            1L -> "zítra"
            2L -> "pozítří"
            in 3L..7L -> when (dayOfWeek!!) {
                DayOfWeek.MONDAY -> "v pondělí"
                DayOfWeek.TUESDAY -> "v úterý"
                DayOfWeek.WEDNESDAY -> "ve středu"
                DayOfWeek.THURSDAY -> "ve čtvrtek"
                DayOfWeek.FRIDAY -> "v pátek"
                DayOfWeek.SATURDAY -> "v sobotu"
                DayOfWeek.SUNDAY -> "v neděli"
            }

            else -> asString()
        }
    }

    fun Smer.toInt(): Int = when (this) {
        Smer.POZITIVNI -> 1
        Smer.NEGATIVNI -> -1
    }

    fun <T> ifTake(condition: Boolean, take: () -> T): T? = if (condition) take() else null

    inline fun <T> List<T>.reversedIf(predicate: (List<T>) -> Boolean): List<T> = if (predicate(this)) this.reversed() else this

    fun <R> funguj(vararg msg: R?) = run { if (BuildConfig.DEBUG) Log.d("funguj", msg.joinToString()) }
    inline fun <reified T : Any?, reified R : Any?, reified S : Any?> T.funguj(vararg msg: R, transform: T.() -> S): T =
        also { UtilFunctions.funguj(this.transform(), *msg) }

    inline fun <reified T : Any?, reified R : Any?> T.funguj(vararg msg: R): T = also { funguj(*msg, transform = { this }) }
    inline fun <reified T : Any?, reified S : Any?> T.funguj(transform: T.() -> S = { this as S }): T =
        also { funguj(*emptyArray<Any?>(), transform = transform) }

    inline fun <reified T : Any?> T.funguj(): T = also { funguj(*emptyArray<Any?>(), transform = { this }) }

    fun Int.toSign() = when (sign) {
        -1 -> "-"
        1 -> "+"
        else -> ""
    }

    fun Float.toSign() = when (sign) {
        -1F -> "-"
        1F -> "+"
        else -> ""
    }

    @Composable
    fun barvaZpozdeniTextu(zpozdeni: Int) = when {
        zpozdeni > 5 -> Color.Red
        zpozdeni > 0 -> Color(0xFFCC6600)
        else -> Color.Green
    }

    @Composable
    fun barvaZpozdeniBublinyText(zpozdeni: Int) = when {
        zpozdeni > 5 -> MaterialTheme.colorScheme.onErrorContainer
        zpozdeni > 0 -> Color(0xFFffddaf)
        else -> Color(0xFFADF0D8)
    }

    @Composable
    fun barvaZpozdeniBublinyKontejner(zpozdeni: Int) = when {
        zpozdeni > 5 -> MaterialTheme.colorScheme.errorContainer
        zpozdeni > 0 -> Color(0xFF614000)
        else -> Color(0xFF015140)
    }

    fun Offset(x: Float = 0F, y: Float = 0F) = androidx.compose.ui.geometry.Offset(x, y)

    @ExperimentalMaterial3Api
    @Composable
    fun IconWithTooltip(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tooltipText: String? = contentDescription,
        tint: Color = LocalContentColor.current,
    ) = if (tooltipText != null) PlainTooltipBox(
        tooltip = {
            DPMCBTheme {
                Text(text = tooltipText)
            }
        }
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier.tooltipTrigger(),
            tint = tint
        )
    }
    else
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )

    inline fun <reified T, R> Iterable<Flow<T>>.combine(crossinline transform: suspend (List<T>) -> R) =
        kotlinx.coroutines.flow.combine(this) { transform(it.toList()) }

    fun String?.toCasDivne() = (this?.run {
        LocalTime.of(slice(0..1).toInt(), slice(2..3).toInt())!!
    } ?: ted)

    fun String?.toCas() = (this?.run {
        val list = split(":").map(String::toInt)
        LocalTime.of(list[0], list[1])!!
    } ?: ted)

    fun String.toDatumDivne() = LocalDate.of(slice(4..7).toInt(), slice(2..3).toInt(), slice(0..1).toInt())!!

    val Context.schemaFile get() = File(filesDir, "schema.pdf")

    val Context.isOnline: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

            return activeNetwork.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) || activeNetwork.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) || activeNetwork.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            )
        }

    @OptIn(ExperimentalTypeInference::class)
    fun <T> Flow<T>.compare(initial: T, @BuilderInference comparation: suspend (oldValue: T, newValue: T) -> T): Flow<T> {
        var oldValue: T = initial
        return flow {
            emit(oldValue)
            collect { newValue ->
                oldValue = comparation(oldValue, newValue)
                emit(oldValue)
            }
        }
    }

    fun LocalDate.asString() = "$dayOfMonth. $monthValue. $year"

    val ted get() = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)!!
    val presneTed get() = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)!!

    val tedFlow = flow {
        while (currentCoroutineContext().isActive) {
            delay(500)
            emit(LocalTime.now().truncatedTo(ChronoUnit.SECONDS))
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(MainScope(), SharingStarted.WhileSubscribed(5_000), ted)

    operator fun LocalTime.plus(duration: kotlin.time.Duration) = plus(duration.toJavaDuration())!!
    operator fun LocalDate.plus(duration: kotlin.time.Duration) = plusDays(duration.inWholeDays)!!

    inline val NavHostController.navigateFunction get() = { it: Direction -> this.funguj().navigate(it.funguj { route }) }
    inline val NavHostController.navigateToRouteFunction get() = { it: String -> this.funguj().navigate(it.funguj()) }
    inline val DestinationsNavigator.navigateFunction get() = { it: Direction -> this.funguj().navigate(it.funguj { route }) }

    fun List<Boolean>.allTrue() = all { it }

    @Composable
    fun darkMode(): Boolean {
        val nastaveni by LocalContext.current.nastaveni.collectAsStateWithLifecycle()
        return if (nastaveni.dmPodleSystemu) isSystemInDarkTheme() else nastaveni.dm
    }
}

typealias NavigateFunction = (Direction) -> Unit
typealias NavigateBackFunction<R> = (R) -> Unit

typealias MutateListFunction<T> = (MutateListLambda<T>) -> Unit
typealias MutateListLambda<T> = MutableList<T>.() -> Unit

typealias MutateFunction<T> = (MutateLambda<T>) -> Unit
typealias MutateLambda<T> = (T) -> T
