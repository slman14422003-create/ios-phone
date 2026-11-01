package com.iosphone.app.ui.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.Contact
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

@Composable
fun ContactDetailScreen(contact: Contact, onBack: () -> Unit) {
    val colors = Ios.colors
    val context = LocalContext.current
    val call = rememberCaller()
    val screenBg = if (colors.dark) colors.background else colors.secondaryBackground
    val cardBg = if (colors.dark) colors.secondaryBackground else colors.background
    val first = contact.phones.firstOrNull()

    Column(Modifier.fillMaxSize().background(screenBg).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.height(44.dp).padding(start = 8.dp).clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                null,
                Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(colors.tint),
            )
            BasicText(stringResource(R.string.contacts_title), style = IosType.body.copy(color = colors.tint))
        }
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(contact.name, contact.photoUri, 110.dp)
            Spacer(Modifier.height(12.dp))
            BasicText(
                contact.name,
                style = IosType.body.copy(
                    color = colors.label,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        if (first != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionButton(Icons.Filled.ChatBubble, stringResource(R.string.message), cardBg, Modifier.weight(1f)) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + first.number)))
                    }
                }
                ActionButton(Icons.Filled.Call, stringResource(R.string.call), cardBg, Modifier.weight(1f)) {
                    call(first.number)
                }
            }
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)).background(cardBg),
            ) {
                contact.phones.forEachIndexed { index, phone ->
                    Column(
                        Modifier.fillMaxWidth().clickable { call(phone.number) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        BasicText(phone.label, style = IosType.footnote.copy(color = colors.secondaryLabel))
                        BasicText(phone.number, style = IosType.body.copy(color = colors.tint))
                    }
                    if (index < contact.phones.lastIndex) {
                        Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = Ios.colors
    Column(
        modifier.height(64.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(icon, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.tint))
        Spacer(Modifier.height(4.dp))
        BasicText(label, style = IosType.tabLabel.copy(color = colors.tint, fontSize = 11.sp))
    }
}
