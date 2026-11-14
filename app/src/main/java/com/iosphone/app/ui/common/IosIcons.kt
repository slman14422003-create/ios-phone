package com.iosphone.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/** iOS (SF Symbols) style icons, drawn as vectors. Tint with ColorFilter. */
object IosIcons {
    private const val HANDSET = "M 4.79,1.31 C 6.21,0.6 7.63,1.31 8.34,2.73 L 9.76,6.28 C 10.12,7.34 9.76,8.41 9.05,9.12 L 7.99,10.18 C 9.41,13.02 11.54,15.15 14.38,16.57 L 15.44,15.51 C 16.15,14.8 17.22,14.45 18.28,14.8 L 21.83,16.22 C 23.25,16.93 23.96,18.35 23.25,19.77 C 22.19,21.9 20.06,22.96 17.93,22.25 C 8.7,19.41 2.66,13.38 0.18,4.15 C -0.18,2.73 3.73,1.67 4.79,1.31 Z"
    private const val HANDSET_SMALL = "M 3.91,6.18 C 5.07,5.6 6.23,6.18 6.81,7.34 L 7.97,10.24 C 8.26,11.11 7.97,11.98 7.39,12.56 L 6.53,13.43 C 7.69,15.75 9.42,17.49 11.74,18.65 L 12.62,17.78 C 13.19,17.2 14.06,16.91 14.94,17.2 L 17.84,18.36 C 18.99,18.94 19.57,20.1 18.99,21.26 C 18.12,23 16.38,23.87 14.64,23.29 C 7.11,20.97 2.17,16.04 0.15,8.5 C -0.14,7.34 3.04,6.47 3.91,6.18 Z"
    private const val BUBBLE = "M12,3 C6.48,3 2,6.9 2,11.65 C2,14.2 3.25,16.5 5.3,18.05 C5.2,19.3 4.6,20.4 3.7,21.2 C3.4,21.5 3.6,22 4,22 C6.2,21.9 8,21.2 9.3,20.2 C10.1,20.4 11,20.5 12,20.5 C17.52,20.5 22,16.4 22,11.65 C22,6.9 17.52,3 12,3 Z"

    private fun build(name: String, body: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(body).build()

    private fun ImageVector.Builder.fill(d: String) {
        addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
    }

    private fun ImageVector.Builder.line(d: String, w: Float = 1.9f) {
        addPath(
            pathData = addPathNodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = w,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    /** phone.fill */
    val Phone: ImageVector by lazy { build("IosPhone") { fill(HANDSET) } }

    /** message.fill */
    val Message: ImageVector by lazy { build("IosMessage") { fill(BUBBLE) } }

    /** All calls */
    val AllCalls: ImageVector get() = Phone

    /** phone.badge.xmark */
    val Missed: ImageVector by lazy {
        build("IosMissed") {
            fill(HANDSET_SMALL)
            line("M15,3 L21,9 M21,3 L15,9")
        }
    }

    /** phone.arrow.up.right */
    val Outgoing: ImageVector by lazy {
        build("IosOutgoing") {
            fill(HANDSET_SMALL)
            line("M14,9.5 L21.5,2 M14.5,2 L21.5,2 L21.5,9")
        }
    }

    /** phone.arrow.down.left */
    val Incoming: ImageVector by lazy {
        build("IosIncoming") {
            fill(HANDSET_SMALL)
            line("M21.5,2 L14,9.5 M14,2.5 L14,9.5 L21,9.5")
        }
    }

    /** voicemail */
    val Voicemail: ImageVector by lazy {
        build("IosVoicemail") {
            line("M3,11 A4,4 0 1 0 11,11 A4,4 0 1 0 3,11 Z M13,11 A4,4 0 1 0 21,11 A4,4 0 1 0 13,11 Z M7,15 L17,15", 1.9f)
        }
    }

    /** waveform */
    /** phone.down.fill (end call) */
    val CallEnd: ImageVector by lazy {
        build("IosCallEnd") {
            group(rotate = 135f, pivotX = 12f, pivotY = 11.6f) { fill(HANDSET) }
        }
    }
}
