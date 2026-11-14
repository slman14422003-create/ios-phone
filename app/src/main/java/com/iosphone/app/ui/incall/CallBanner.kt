package com.iosphone.app.ui.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.IosIcons
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.ui.theme.ltr
import com.iosphone.app.util.PhoneFormat

@Composable
fun IncomingBanner(
    visible: Boolean,
    name: String,
    number: String,
    photo: String?,
    decline: Color,
    accept: Color,
    onExpand: () -> Unit,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().statusBarsPadding()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(tween(340)) { -it * 2 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(260)) { -it * 2 } + fadeOut(tween(160)),
        ) {
            val shape = RoundedCornerShape(32.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .shadow(18.dp, shape, clip = false)
                    .background(Color(0xF21C1C1E), shape)
                    .pressable(pressedScale = 0.985f) { onExpand() }
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(name, photo, 48.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    BasicText(
                        name,
                        style = IosType.headline.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = PhoneFormat.format(number)
                    BasicText(
                        if (sub.isNotBlank() && sub != name) sub else stringResource(R.string.mobile),
                        style = IosType.footnote.copy(color = Color.White.copy(alpha = 0.62f)).ltr(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BannerButton(IosIcons.CallEnd, decline, onDecline)
                Spacer(Modifier.width(12.dp))
                BannerButton(IosIcons.Phone, accept, onAccept)
            }
        }
    }
}

@Composable
private fun BannerButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .pressable(pressedScale = 0.9f, onClick = onClick)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, null, Modifier.size(21.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}
