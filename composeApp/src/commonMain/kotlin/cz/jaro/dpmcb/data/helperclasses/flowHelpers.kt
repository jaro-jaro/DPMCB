@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Returns a [kotlinx.coroutines.flow.Flow] whose values are generated with [transform] function by combining
 * the most recently emitted values by each flow.
 */
@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, R : Any> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

/**
 * Returns a [Flow] whose values are generated with [transform] function by combining
 * the most recently emitted values by each flow.
 */
fun <T1, T2, T3, R> Flow<T1>.combine(
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    transform: suspend (T1, T2, T3) -> R,
): Flow<R> = combine(this, flow2, flow3, transform)

/**
 * Returns a [Flow] whose values are generated with [transform] function by combining
 * the most recently emitted values by each flow.
 */
@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, R : Any> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
    )
}

/**
 * Returns a [Flow] whose values are generated with [transform] function by combining
 * the most recently emitted values by each flow.
 */
@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, T8, R : Any> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
        args[7] as T8,
    )
}

inline fun <reified T, R> Iterable<Flow<T>>.combineAll(crossinline transform: suspend (List<T>) -> R) =
    combine(this) { transform(it.toList()) }


@OptIn(ExperimentalTypeInference::class)
fun <T : U, U, R> Flow<T>.compare(initial: U, @BuilderInference compare: suspend (oldValue: U, newValue: T) -> R): Flow<R> {
    var oldValue: U = initial
    return flow {
        collect { newValue ->
            emit(compare(oldValue, newValue))
            oldValue = newValue
        }
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <T, R> Flow<T>.compare(@BuilderInference compare: suspend (oldValue: T?, newValue: T) -> R): Flow<R> {
    var oldValue: T? = null
    return flow {
        collect { newValue ->
            emit(compare(oldValue, newValue))
            oldValue = newValue
        }
    }
}

fun <T> (suspend () -> T).asRepeatingFlow(duration: Duration = 500.milliseconds): Flow<T> = flow {
    while (currentCoroutineContext().isActive) {
        emit(invoke())
        delay(duration)
    }
}

fun <T> (() -> T).asRepeatingFlow(duration: Duration = 500.milliseconds): Flow<T> = suspend { invoke() }.asRepeatingFlow(duration)

fun <T> (() -> T).asRepeatingStateFlow(
    scope: CoroutineScope,
    started: SharingStarted,
    duration: Duration = 500.milliseconds,
): StateFlow<T> = asRepeatingFlow(duration).stateIn(scope, started, invoke())


/**
 * Performs the given [action] on each element.
 */
suspend inline fun <T> ReceiveChannel<T>.forEach(action: (T) -> Unit) {
    for (element in this) action(element)
}


context(vm: ViewModel)
fun <T> Flow<T>.stateInViewModel(
    started: SharingStarted,
    initialValue: T
): StateFlow<T> = stateIn(vm.viewModelScope, started, initialValue)

context(vm: ViewModel)
fun <T> async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = vm.viewModelScope.async(context, start, block)

context(vm: ViewModel)
fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = vm.viewModelScope.launch(context, start, block)

context(vm: ViewModel)
fun <T> Flow<T>.launch(): Job = launchIn(vm.viewModelScope)