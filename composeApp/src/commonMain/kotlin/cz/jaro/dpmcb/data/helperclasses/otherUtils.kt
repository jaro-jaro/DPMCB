@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cz.jaro.dpmcb.data.helperclasses

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import dev.gitlive.firebase.database.DatabaseReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

fun Offset(x: Float = 0F, y: Float = 0F) = androidx.compose.ui.geometry.Offset(x, y)

expect val Dispatchers.IO: CoroutineDispatcher

expect val isDebug: Boolean

expect suspend fun awaitFrame()

expect fun String.encodeURL(): String

private const val OnSecondaryClickModifierNoParamError =
    "Modifier.onSecondaryClick must provide one or more 'key' parameters that define the identity of " +
            "the modifier and determine when its previous input processing coroutine should be " +
            "cancelled and a new effect launched for the new key."
// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Suppress("UnusedReceiverParameter")
@Deprecated(OnSecondaryClickModifierNoParamError, level = DeprecationLevel.ERROR)
fun Modifier.onSecondaryClick(onClick: (event: PointerEvent) -> Unit): Modifier =
    error(OnSecondaryClickModifierNoParamError)

fun Modifier.onSecondaryClick(vararg keys: Any?, onClick: (event: PointerEvent) -> Unit) =
    pointerInput(*keys) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed)
                        onClick(event)
                }
            }
        }
    }

expect suspend inline fun <reified T> DatabaseReference.getValue(): T

expect val maxDatabaseInsertBatchSize: Int

expect val backgroundInfo: String