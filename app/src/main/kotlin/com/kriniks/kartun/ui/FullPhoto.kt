package com.kriniks.kartun.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kriniks.kartun.R
import kotlin.math.abs

private const val MAX_ZOOM = 8f
/** One double-tap step: ×2.5 at the tapped point, the same step as the preview's double tap (SetupScreen PREVIEW_ZOOM). */
private const val DOUBLE_TAP_ZOOM = 2.5f
/** A vertical swipe this long on the un-zoomed photo closes the viewer. */
private val CLOSE_SWIPE = 120.dp

/**
 * The picked photo full screen on black (plans/03 step 9, the owner's «раскрывать вью картинки на весь экран»):
 * two fingers zoom and pan; on the un-zoomed photo a vertical swipe closes it, so does Back and the round back button.
 * Opening and closing: [TESTED: 2026-10-14 · by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md runs 16-17].
 * Pinch zoom: [NOT-TESTED] (adb input has no multi-touch).
 */
@Composable
fun FullPhoto(bitmap: Bitmap, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        var zoom by remember { mutableFloatStateOf(1f) }
        var pan by remember { mutableStateOf(Offset.Zero) }
        // Double tap (the owner, 2026-10-16 ≈ 13:2x: «дабл тап во фул скрин превью фото увеличивал фото» · «а повторный дабл
        // тап - уменьшал на шаг назад»): a double tap zooms DOUBLE_TAP_ZOOM× more at the tapped point and remembers where it
        // was; the next double tap goes back to that zoom and pan.
        // [TESTED: 2026-10-16 · Titan - testcases/reports/2026-10-16_fullphoto_double_tap.md]
        var before by remember { mutableStateOf<Pair<Float, Offset>?>(null) }
        val closePx = with(LocalDensity.current) { CLOSE_SWIPE.toPx() }
        Box(
            Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { at ->
                    val back = before
                    if (back != null) {
                        zoom = back.first; pan = back.second; before = null
                    } else {
                        before = zoom to pan
                        val next = (zoom * DOUBLE_TAP_ZOOM).coerceAtMost(MAX_ZOOM)
                        // Keep the tapped point under the finger: with the scale around the centre, a point d from the
                        // centre lands at d * k + pan, so pan grows by (centre - at) * (k - 1) scaled from the old zoom.
                        val centre = Offset(size.width / 2f, size.height / 2f)
                        val k = next / zoom
                        val p = (pan - (at - centre)) * k + (at - centre)
                        val maxX = size.width * (next - 1) / 2
                        val maxY = size.height * (next - 1) / 2
                        zoom = next
                        pan = Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
                    }
                })
            }.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var dragY = 0f
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2 || zoom > 1f) {
                            zoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            val maxX = size.width * (zoom - 1) / 2
                            val maxY = size.height * (zoom - 1) / 2
                            val p = pan + event.calculatePan()
                            pan = Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
                        } else {
                            dragY += event.calculatePan().y
                            pan = Offset(0f, dragY)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (zoom == 1f) {
                        if (abs(dragY) > closePx) onClose()
                        pan = Offset.Zero
                    }
                }
            },
        ) {
            Image(
                bitmap.asImageBitmap(), null,
                Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y },
                contentScale = ContentScale.Fit,
            )
            RoundIconButton(R.drawable.ic_back, onClick = onClose, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(20.dp), translucent = true)
        }
    }
}
