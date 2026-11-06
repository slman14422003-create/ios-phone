package com.iosphone.app.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
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
import com.iosphone.app.ui.calls.CallDetailScreen
import com.iosphone.app.ui.calls.VoicemailScreen
import com.iosphone.app.ui.contacts.ContactDetailScreen
import com.iosphone.app.ui.contacts.ContactEditorScreen
import com.iosphone.app.ui.recordings.RecordingsScreen
import com.iosphone.app.ui.settings.SettingsScreen

sealed interface Route {
    data class ContactDetail(val id: Long) : Route
    data class CallInfo(val number: String) : Route
    data class Editor(val id: Long?, val number: String?) : Route
    data class Recordings(val contactId: Long?) : Route
    data object Voicemail : Route
    data object Settings : Route
}

class Nav {
    val stack = mutableStateListOf<Route>()

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }
}

val LocalNav = staticCompositionLocalOf<Nav> { error("Nav missing") }

private val PushEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val PushMs = 420

private data class Page(val route: Route?, val depth: Int)

@Composable
fun RouteLayer(nav: Nav, modifier: Modifier = Modifier) {
    val page = Page(nav.stack.lastOrNull(), nav.stack.size)
    BackHandler(enabled = page.route != null) { nav.pop() }
    AnimatedContent(
        targetState = page,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            val slideIn = slideInHorizontally(tween(PushMs, easing = PushEase)) { it }
            val slideOut = slideOutHorizontally(tween(PushMs, easing = PushEase)) { it }
            val parallaxIn = slideInHorizontally(tween(PushMs, easing = PushEase)) { -it / 3 } + fadeIn(tween(PushMs))
            val parallaxOut = slideOutHorizontally(tween(PushMs, easing = PushEase)) { -it / 3 } + fadeOut(tween(PushMs))
            when {
                initialState.route == null -> (slideIn togetherWith fadeOut(tween(PushMs))).also { it.targetContentZIndex = 1f }
                targetState.route == null -> (fadeIn(tween(PushMs)) togetherWith slideOut).also { it.targetContentZIndex = -1f }
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
                    is Route.ContactDetail -> ContactDetailScreen(route.id)
                    is Route.CallInfo -> CallDetailScreen(route.number)
                    is Route.Editor -> ContactEditorScreen(route.id, route.number)
                    is Route.Recordings -> RecordingsScreen(route.contactId)
                    Route.Voicemail -> VoicemailScreen()
                    Route.Settings -> SettingsScreen()
                }
            }
        } else {
            Box(Modifier.fillMaxSize())
        }
    }
}
