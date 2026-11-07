package com.iosphone.app.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.VOICEMAIL
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

@Composable
fun VoicemailScreen() {
    val colors = Ios.colors
    val nav = LocalNav.current
    val call = rememberCaller()
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { nav.pop() }, iconSize = 28.dp)
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                stringResource(R.string.voicemail_title),
                style = IosType.largeTitle.copy(color = colors.label, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(colors.tint)
                    .pressable(pressedScale = 0.97f) { call(VOICEMAIL) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(stringResource(R.string.call_voicemail), style = IosType.headline.copy(color = Color.White))
            }
        }
    }
}
