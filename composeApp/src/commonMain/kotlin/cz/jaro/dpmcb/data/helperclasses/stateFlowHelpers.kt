@file:Suppress("unused")

package cz.jaro.dpmcb.data.helperclasses

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

inline fun <reified T, R> combineStates(
    coroutineScope: CoroutineScope,
    flows: Iterable<StateFlow<T>>,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    crossinline transform: (Array<T>) -> R
) = combine(flows, transform)
    .stateIn(coroutineScope, sharingStarted, transform(flows.map { it.value }.toTypedArray()))

inline fun <T, R> StateFlow<T>.mapState(
    coroutineScope: CoroutineScope,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    crossinline transform: (value: T) -> R,
): StateFlow<R> = map(transform)
    .stateIn(coroutineScope, sharingStarted, transform(value))

inline fun <T> StateFlow<T>.filterState(
    coroutineScope: CoroutineScope,
    defaultInitialValue: T,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    crossinline predicate: (value: T) -> Boolean,
): StateFlow<T> = filter(predicate)
    .stateIn(coroutineScope, sharingStarted, if (predicate(value)) value else defaultInitialValue)

fun <T : Any> StateFlow<T?>.filterNotNullState(
    coroutineScope: CoroutineScope,
    defaultInitialValue: T,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
): StateFlow<T> = filterNotNull()
    .stateIn(coroutineScope, sharingStarted, value ?: defaultInitialValue)

fun <T1, T2, R> StateFlow<T1>.combineStates(
    coroutineScope: CoroutineScope,
    flow2: StateFlow<T2>,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    transform: (a: T1, b: T2) -> R,
): StateFlow<R> = combineStates(coroutineScope, this, flow2, sharingStarted, transform)

fun <T1, T2, R> combineStates(
    coroutineScope: CoroutineScope,
    flow: StateFlow<T1>,
    flow2: StateFlow<T2>,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    transform: (a: T1, b: T2) -> R,
): StateFlow<R> = flow.combine(flow2, transform)
    .stateIn(coroutineScope, sharingStarted, transform(flow.value, flow2.value))

fun <T1, T2, T3, R> combineStates(
    coroutineScope: CoroutineScope,
    flow: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    transform: (T1, T2, T3) -> R,
): StateFlow<R> = combine(flow, flow2, flow3, transform)
    .stateIn(coroutineScope, sharingStarted, transform(flow.value, flow2.value, flow3.value))

fun <T1, T2, T3, T4, R> combineStates(
    coroutineScope: CoroutineScope,
    flow: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    flow4: StateFlow<T4>,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    transform: (T1, T2, T3, T4) -> R,
): StateFlow<R> = combine(flow, flow2, flow3, flow4, transform)
    .stateIn(coroutineScope, sharingStarted, transform(flow.value, flow2.value, flow3.value, flow4.value))

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun <T> StateFlow<StateFlow<T>>.flattenMergeStates(
    coroutineScope: CoroutineScope,
    sharingStarted: SharingStarted = SharingStarted.Eagerly,
    concurrency: Int = DEFAULT_CONCURRENCY
) = flattenMerge(concurrency)
    .stateIn(coroutineScope, sharingStarted, value.value)