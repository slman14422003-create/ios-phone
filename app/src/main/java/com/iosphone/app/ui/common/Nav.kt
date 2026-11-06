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
import com.iosphone.app.ui.calls.CallDetailScreen
import com.iosphone.app.ui.calls.VoicemailScreen
import com.iosphone.app.ui.contacts.ContactDetailScreen
import com.iosphone.app.ui.contacts.ContactEditorScreen

sealed interface Route {
    data class ContactDetail(val id: Long) : Route
    data class CallInfo(val number: String) : Route
    data class Editor(val id: Long?, val number: String?) : Route
    data object Voicemail : Route
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

@Composable
fun RouteLayer(nav: Nav, modifier: Modifier = Modifier) {
    val top = nav.stack.lastOrNull()
    BackHandler(enabled = top != null) { nav.pop() }
    AnimatedContent(
        targetState = top,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            when {
                initialState == null -> slideInHorizontally(tween(340)) { it } togetherWith fadeOut(tween(200))
                targetState == null -> fadeIn(tween(200)) togetherWith slideOutHorizontally(tween(340)) { it }
                else -> slideInHorizontally(tween(340)) { it } togetherWith slideOutHorizontally(tween(340)) { -it / 3 }
            }
        },
        label = "route",
    ) { route ->
        if (route != null) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
                when (route) {
                    is Route.ContactDetail -> ContactDetailScreen(route.id)
                    is Route.CallInfo -> CallDetailScreen(route.number)
                    is Route.Editor -> ContactEditorScreen(route.id, route.number)
                    Route.Voicemail -> VoicemailScreen()
                }
            }
        } else {
            Box(Modifier.fillMaxSize())
        }
    }
}
