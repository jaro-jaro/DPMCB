package cz.jaro.dpmcb.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import cz.jaro.dpmcb.ui.main.Route

context(MySharedTransitionScope)
inline fun <reified T : Route> NavGraphBuilder.route(
    crossinline transitionContent: @Composable TransitionScope.(T) -> Unit,
) = route<T>(content = {
    val scope: TransitionScope = object : TransitionScope, AnimatedVisibilityScope by this, MySharedTransitionScope by this@MySharedTransitionScope {}
    scope.transitionContent(it)
})

@OptIn(ExperimentalSharedTransitionApi::class)
interface MySharedTransitionScope : SharedTransitionScope {
    var sharedTransitionData: Any?
}

interface TransitionScope : MySharedTransitionScope, AnimatedVisibilityScope

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun MySharedTransitionLayout(
    content: @Composable MySharedTransitionScope.() -> Unit,
) = SharedTransitionLayout {
    val transitionScope: MySharedTransitionScope = remember {
        object : MySharedTransitionScope, SharedTransitionScope by this {
            override var sharedTransitionData by mutableStateOf(null as Any?)
        }
    }
    transitionScope.content()
}

context(TransitionScope)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElement(key: Any) = sharedElement(
    state = rememberSharedContentState(key),
    animatedVisibilityScope = this@TransitionScope,
)