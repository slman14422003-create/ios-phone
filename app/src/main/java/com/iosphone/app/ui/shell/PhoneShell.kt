package com.iosphone.app.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import android.view.HapticFeedbackConstants
import com.iosphone.app.ui.common.ScreenBlur
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.ViewModel
import com.iosphone.app.ui.common.GlassBox
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.rememberAppData
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.calls.CallsScreen
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.GlassSearchField
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.Nav
import com.iosphone.app.ui.common.RequestPermissionsOnce
import com.iosphone.app.ui.common.RouteLayer
import com.iosphone.app.ui.common.glass
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.contacts.ContactsScreen
import com.iosphone.app.ui.keypad.KeypadScreen
import com.iosphone.app.ui.search.SearchScreen
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

private class TabSpec(val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabSpec(R.string.tab_calls, Icons.Filled.AccessTime),
    TabSpec(R.string.tab_contacts, Icons.Filled.AccountCircle),
    TabSpec(R.string.tab_keypad, Icons.Filled.Dialpad),
)

/** Shell state lives in a ViewModel because the content and the glass bar are two separate views (see MainActivity). */
class ShellViewModel : ViewModel() {
    var tab by mutableIntStateOf(0)
    var searching by mutableStateOf(false)
    var query by mutableStateOf("")
    val nav = Nav()
}

private val IosEase = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)

/** Everything that scrolls. Lives inside the BlurTarget so the bar can blur it. */
@Composable
fun PhoneContent(vm: ShellViewModel, dialNumber: String?) {
    val colors = Ios.colors
    val data = rememberAppData()
    val nav = vm.nav
    CompositionLocalProvider(LocalAppData provides data, LocalNav provides nav) {
        RequestPermissionsOnce()
        val menuOpen = ScreenBlur.count > 0
        val menuBlur by animateDpAsState(if (menuOpen) 16.dp else 0.dp, tween(240, easing = IosEase), label = "menuBlur")
        val menuDim by animateFloatAsState(if (menuOpen) 0.3f else 0f, tween(240, easing = IosEase), label = "menuDim")
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (menuBlur > 0.dp) Modifier.blur(menuBlur, BlurredEdgeTreatment.Unbounded) else Modifier),
            ) {
                AnimatedContent(
                    targetState = if (vm.searching) 3 else vm.tab,
                    transitionSpec = {
                        (fadeIn(tween(260, easing = IosEase)) + scaleIn(tween(260, easing = IosEase), initialScale = 0.985f)) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "tabs",
                ) { page ->
                    when (page) {
                        0 -> CallsScreen()
                        1 -> ContactsScreen()
                        2 -> KeypadScreen(dialNumber)
                        else -> SearchScreen(vm.query)
                    }
                }
                RouteLayer(nav)
            }
            if (menuDim > 0.001f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = menuDim)))
            }
        }
    }
}

/** The floating glass tab bar / search field. Sits above the BlurTarget. */
@Composable
fun PhoneChrome(vm: ShellViewModel) {
    AnimatedVisibility(
        visible = vm.nav.stack.isEmpty(),
        enter = fadeIn(tween(220)) + slideInVertically(tween(300, easing = IosEase)) { it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(240, easing = IosEase)) { it / 2 },
    ) {
        BottomBar(
            tab = vm.tab,
            searching = vm.searching,
            query = vm.query,
            onQuery = { vm.query = it },
            onTab = {
                vm.tab = it
                vm.searching = false
                vm.query = ""
            },
            onSearching = {
                vm.searching = it
                if (!it) vm.query = ""
            },
        )
    }
}

@Composable
private fun BottomBar(
    tab: Int,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onTab: (Int) -> Unit,
    onSearching: (Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = searching,
        transitionSpec = { (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f)) togetherWith fadeOut(tween(140)) },
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
        label = "bar",
    ) { isSearching ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSearching) {
                GlassSearchField(query, onQuery, Modifier.weight(1f), autoFocus = true)
                GlassIconButton(Icons.Filled.Close, onClick = { onSearching(false) }, size = 62.dp, iconSize = 24.dp)
            } else {
                TabPill(tab, onTab, Modifier.weight(1f))
                GlassIconButton(Icons.Filled.Search, onClick = { onSearching(true) }, size = 62.dp, iconSize = 26.dp)
            }
        }
    }
}

@Composable
private fun TabPill(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Ios.colors
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(0f) }
    val tabPx = widthPx / tabs.size
    val pillX = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(selected, tabPx) {
        if (!dragging && tabPx > 0f) {
            pillX.animateTo(tabPx * selected, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val hover by remember(tabPx) {
        derivedStateOf {
            if (tabPx <= 0f) 0 else ((pillX.value + tabPx / 2f) / tabPx).toInt().coerceIn(0, tabs.lastIndex)
        }
    }
    LaunchedEffect(hover) {
        if (dragging) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    val grow by animateFloatAsState(
        targetValue = if (dragging) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "pillGrow",
    )
    val shownIndex = if (dragging) hover else selected

    fun settle() {
        dragging = false
        if (tabPx <= 0f) return
        val target = (pillX.value / tabPx).roundToInt().coerceIn(0, tabs.lastIndex)
        scope.launch {
            pillX.animateTo(tabPx * target, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium))
        }
        if (target != currentSelected) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            currentOnSelect(target)
        }
    }

    GlassBox(
        CircleShape,
        modifier
            .height(62.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(tabPx) {
                detectTapGestures { o ->
                    if (tabPx > 0f) currentOnSelect((o.x / tabPx).toInt().coerceIn(0, tabs.lastIndex))
                }
            }
            .pointerInput(tabPx) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { settle() },
                    onDragCancel = { settle() },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        val maxX = tabPx * tabs.lastIndex
                        scope.launch { pillX.snapTo((pillX.value + dx).coerceIn(0f, maxX)) }
                    },
                )
            },
    ) {
        if (tabPx > 0f) {
            val tabWidth = with(density) { tabPx.toDp() }
            Box(
                Modifier
                    .offset { IntOffset(pillX.value.roundToInt(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = grow
                        scaleY = grow
                    }
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(if (dragging) colors.selection.copy(alpha = 0.7f) else colors.selection),
            )
        }
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, spec ->
                val active = index == shownIndex
                val tint by animateColorAsState(
                    targetValue = if (active) colors.tint else colors.label,
                    animationSpec = tween(160),
                    label = "tabTint",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.1f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                    label = "tabIcon",
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        spec.icon,
                        null,
                        Modifier.size(24.dp).graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                        colorFilter = ColorFilter.tint(tint),
                    )
                    BasicText(stringResource(spec.label), style = IosType.tabLabel.copy(color = tint))
                }
            }
        }
    }
}
