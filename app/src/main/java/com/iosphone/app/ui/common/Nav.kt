package com.iosphone.app.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.iosphone.app.ui.calls.CallDetailScreen
import com.iosphone.app.ui.calls.VoicemailScreen
import com.iosphone.app.ui.settings.AnswerEndScreen
import com.iosphone.app.ui.settings.BlockedScreen
import com.iosphone.app.ui.settings.CallStyleScreen
import com.iosphone.app.ui.settings.OtherCallsScreen
import com.iosphone.app.ui.settings.QuickRepliesScreen
import com.iosphone.app.ui.settings.SettingsScreen
import com.iosphone.app.ui.settings.SpeedDialScreen
import com.iosphone.app.ui.settings.SupplementaryScreen

sealed interface Route {
    data class CallInfo(val number: String) : Route
    data object Voicemail : Route
    data object Settings : Route
    data class CallStyle(val incoming: Boolean) : Route
    data object QuickReplies : Route
    data object SpeedDial : Route
    data object Blocked : Route
    data object AnswerEnd : Route
    data object Supplementary : Route
    data object OtherCalls : Route
}

class Nav {
    val stack = mutableStateListOf<Route>()
    var returnOnEmpty = false
    var onEmpty: (() -> Unit)? = null

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isEmpty()) return
        stack.removeAt(stack.lastIndex)
        if (stack.isEmpty() && returnOnEmpty) {
            returnOnEmpty = false
            onEmpty?.invoke()
        }
    }
}

val LocalNav = staticCompositionLocalOf<Nav> { error("Nav missing") }

private data class Page(val route: Route?, val depth: Int)

@Composable
fun RouteLayer(nav: Nav, modifier: Modifier = Modifier) {
    val page = Page(nav.stack.lastOrNull(), nav.stack.size)
    val sign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val ms = Motion.PushMs
    val ease = Motion.PushEase
    BackHandler(enabled = page.route != null) { nav.pop() }
    AnimatedContent(
        targetState = page,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            val slideIn = slideInHorizontally(tween(ms, easing = ease)) { it * sign }
            val slideOut = slideOutHorizontally(tween(ms, easing = ease)) { it * sign }
            val parallaxIn = slideInHorizontally(tween(ms, easing = ease)) { -it * sign / 3 } + fadeIn(tween(ms))
            val parallaxOut = slideOutHorizontally(tween(ms, easing = ease)) { -it * sign / 3 } + fadeOut(tween(ms))
            when {
                initialState.route == null -> (slideIn togetherWith fadeOut(tween(ms))).also { it.targetContentZIndex = 1f }
                targetState.route == null -> (fadeIn(tween(ms)) togetherWith slideOut).also { it.targetContentZIndex = -1f }
                forward -> (slideIn togetherWith parallaxOut).also { it.targetContentZIndex = 1f }
                else -> (parallaxIn togetherWith slideOut).also { it.targetContentZIndex = -1f }
            }
        },
        label = "route",
    ) { p ->
        val route = p.route
        if (route != null) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
                when (route) {
                    is Route.CallInfo -> CallDetailScreen(route.number)
                    Route.Voicemail -> VoicemailScreen()
                    Route.Settings -> SettingsScreen()
                    is Route.CallStyle -> CallStyleScreen(route.incoming)
                    Route.QuickReplies -> QuickRepliesScreen()
                    Route.SpeedDial -> SpeedDialScreen()
                    Route.Blocked -> BlockedScreen()
                    Route.AnswerEnd -> AnswerEndScreen()
                    Route.Supplementary -> SupplementaryScreen()
                    Route.OtherCalls -> OtherCallsScreen()
                }
            }
        } else {
            Box(Modifier.fillMaxSize())
        }
    }
}
