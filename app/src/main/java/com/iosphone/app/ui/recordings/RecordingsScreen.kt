package com.iosphone.app.ui.recordings

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.data.Recording
import com.iosphone.app.data.Recordings
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat
import java.io.File
import kotlinx.coroutines.delay

private class RecordingPlayer {
    var path by mutableStateOf<String?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var position by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set
    private var mp: MediaPlayer? = null

    fun toggle(file: File) {
        if (path == file.absolutePath) {
            val p = mp ?: return
            if (p.isPlaying) {
                p.pause()
                playing = false
            } else {
                p.start()
                playing = true
            }
            return
        }
        release()
        val p = MediaPlayer()
        val ok = runCatching {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            p.setDataSource(file.absolutePath)
            p.prepare()
            p.start()
        }.isSuccess
        if (!ok) {
            runCatching { p.release() }
            return
        }
        p.setOnCompletionListener {
            playing = false
            position = 0
            runCatching { it.seekTo(0) }
        }
        mp = p
        path = file.absolutePath
        duration = runCatching { p.duration }.getOrDefault(0)
        position = 0
        playing = true
    }

    fun seek(ms: Int) {
        mp?.let { runCatching { it.seekTo(ms) } }
        position = ms
    }

    fun tick() {
        val p = mp ?: return
        position = runCatching { p.currentPosition }.getOrDefault(position)
    }

    fun release() {
        runCatching { mp?.release() }
        mp = null
        path = null
        playing = false
        position = 0
        duration = 0
    }
}

@Composable
fun RecordingsScreen(contactId: Long?) {
    val colors = Ios.colors
    val context = LocalContext.current
    val data = LocalAppData.current
    val nav = LocalNav.current
    val contact = remember(contactId, data.contacts) { contactId?.let { data.contactById(it) } }
    val keys = remember(contact) { contact?.phones?.map { PhoneFormat.key(it.number) }?.toSet().orEmpty() }
    var reload by remember { mutableIntStateOf(0) }
    val all by produceState(emptyList<Recording>(), reload) { value = Recordings.load(context) }
    val shown = remember(all, contactId, keys) {
        if (contactId == null) all else all.filter { it.key.isNotEmpty() && it.key in keys }
    }
    val player = remember { RecordingPlayer() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(player.playing) {
        while (player.playing) {
            player.tick()
            delay(200)
        }
    }
    val screenBg = if (colors.dark) colors.background else colors.grouped
    val unknown = stringResource(R.string.unknown)

    Column(Modifier.fillMaxSize().background(screenBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { nav.pop() }, iconSize = 28.dp)
        }
        BasicText(
            stringResource(R.string.call_recordings),
            style = IosType.largeTitle.copy(color = colors.label),
            modifier = Modifier.padding(start = 20.dp, top = 4.dp),
        )
        if (contact != null) {
            BasicText(
                contact.name,
                style = IosType.body.copy(color = colors.secondaryLabel),
                modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText(
                    stringResource(R.string.no_recordings),
                    style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.file.name }, contentType = { "recording" }) { rec ->
                    val title = remember(rec, data.contacts) {
                        data.contactFor(rec.number)?.name
                            ?: if (rec.key.isEmpty()) unknown else PhoneFormat.format(rec.number)
                    }
                    RecordingCard(
                        rec = rec,
                        title = title,
                        player = player,
                        onDelete = {
                            if (player.path == rec.file.absolutePath) player.release()
                            rec.file.delete()
                            reload++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(rec: Recording, title: String, player: RecordingPlayer, onDelete: () -> Unit) {
    val colors = Ios.colors
    val duration by produceState(0L, rec.file) { value = Recordings.durationMs(rec.file) }
    val current = player.path == rec.file.absolutePath
    val playingThis = current && player.playing
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .pressable(pressedScale = 0.92f) { player.toggle(rec.file) }
                    .background(colors.tint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    if (playingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null,
                    Modifier.size(26.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    title,
                    style = IosType.body.copy(color = colors.label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        if (rec.incoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                        null,
                        Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(colors.secondaryLabel),
                    )
                    Spacer(Modifier.width(4.dp))
                    val len = Formats.timer(duration / 1000)
                    BasicText(
                        Formats.fullDate(rec.time) + if (duration > 0) "  ·  $len" else "",
                        style = IosType.callout.copy(color = colors.secondaryLabel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier.size(40.dp).pressable(pressedScale = 0.9f, onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Image(Icons.Filled.Delete, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.red))
            }
        }
        if (current) {
            Spacer(Modifier.height(6.dp))
            val total = player.duration.coerceAtLeast(1)
            SeekBar(progress = player.position.toFloat() / total) { f -> player.seek((f * total).toInt()) }
            Row(Modifier.fillMaxWidth()) {
                BasicText(
                    Formats.timer(player.position / 1000L),
                    style = IosType.footnote.copy(color = colors.secondaryLabel),
                )
                Spacer(Modifier.weight(1f))
                BasicText(
                    Formats.timer(player.duration / 1000L),
                    style = IosType.footnote.copy(color = colors.secondaryLabel),
                )
            }
        }
    }
}

@Composable
private fun SeekBar(progress: Float, onSeek: (Float) -> Unit) {
    val colors = Ios.colors
    var width by remember { mutableFloatStateOf(1f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { onSeek((it.x / width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.fill))
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(colors.tint))
    }
}
