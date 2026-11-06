package com.iosphone.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

@Composable
fun GlassSearchField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    val colors = Ios.colors
    val focus = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
    GlassBox(RoundedCornerShape(31.dp), modifier.height(62.dp), contentAlignment = Alignment.CenterStart) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(Icons.Filled.Search, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                BasicText(
                    stringResource(R.string.search),
                    style = IosType.body.copy(color = colors.secondaryLabel),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = IosType.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.tint),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (value.isNotEmpty()) {
            Image(
                Icons.Filled.Cancel,
                null,
                Modifier.size(20.dp).clickable { onChange("") },
                colorFilter = ColorFilter.tint(colors.secondaryLabel),
            )
        }
    }
    }
}

@Composable
fun Hairline(startPadding: androidx.compose.ui.unit.Dp = 16.dp, endPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(Modifier.padding(start = startPadding, end = endPadding).fillMaxWidth().height(0.5.dp).background(Ios.colors.separator))
}

@Composable
fun FieldRow(
    hint: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    val colors = Ios.colors
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
        if (value.isEmpty()) {
            BasicText(hint, style = IosType.body.copy(color = colors.secondaryLabel))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            keyboardOptions = keyboard,
            textStyle = IosType.body.copy(color = colors.label),
            cursorBrush = SolidColor(colors.tint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
