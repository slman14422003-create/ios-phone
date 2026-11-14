package com.iosphone.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.Contact
import com.iosphone.app.util.PhoneFormat
import com.iosphone.app.util.QuickCopy

/** Places a popup under (or above, when there is no room) its anchor and keeps it inside the window. */
class ClampedBelowProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/**
 * Long-press menu for a person or a number: copy the number(s), email, name or the whole contact card.
 * [extra] items are appended (select, call, message...).
 */
@Composable
fun QuickMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    contact: Contact?,
    number: String?,
    extra: List<MenuItem> = emptyList(),
) {
    if (!expanded) return
    val context = LocalContext.current
    val copyNumber = stringResource(R.string.copy_number)
    val copyName = stringResource(R.string.copy_name)
    val copyEmail = stringResource(R.string.copy_email)
    val copyContact = stringResource(R.string.copy_contact)
    val copyFmt = stringResource(R.string.copy_x)
    val numbers = buildList {
        if (!number.isNullOrEmpty()) add(number)
        contact?.phones?.forEach { p -> if (none { PhoneFormat.key(it) == PhoneFormat.key(p.number) }) add(p.number) }
    }.take(3)
    val items = ArrayList<MenuItem>()
    numbers.forEach { n ->
        val label = if (numbers.size == 1) copyNumber else String.format(copyFmt, PhoneFormat.format(n))
        items.add(MenuItem(label, icon = Icons.Filled.ContentCopy) { QuickCopy.number(context, n) })
    }
    contact?.emails?.firstOrNull()?.let { e ->
        items.add(MenuItem(copyEmail, icon = Icons.Filled.Mail) { QuickCopy.text(context, e.address) })
    }
    if (contact != null) {
        items.add(MenuItem(copyName, icon = Icons.Filled.Person) { QuickCopy.text(context, contact.name) })
        items.add(MenuItem(copyContact, icon = Icons.Filled.ContentCopy) { QuickCopy.text(context, QuickCopy.card(contact)) })
    }
    items.addAll(extra)
    if (items.isEmpty()) return
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.roundToPx() }
    GlassMenu(
        expanded = true,
        onDismiss = onDismiss,
        items = items,
        width = 270.dp,
        positionProvider = ClampedBelowProvider(gap),
    )
}
