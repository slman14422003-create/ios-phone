package com.iosphone.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

@Composable
fun ActionRow(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = Ios.colors
    Box(
        Modifier.fillMaxWidth().height(52.dp).iosRow(onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = IosType.body.copy(color = if (destructive) colors.red else colors.tint))
    }
}

/** Localised name for a phone label key; custom labels are shown as they are. */
@Composable
fun labelText(label: String): String = when (label.trim().lowercase()) {
    "", "mobile" -> stringResource(R.string.label_mobile)
    "home" -> stringResource(R.string.label_home)
    "work" -> stringResource(R.string.label_work)
    "main" -> stringResource(R.string.label_main)
    "other" -> stringResource(R.string.label_other)
    else -> label
}
