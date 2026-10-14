package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R
import com.kriniks.kartun.ResultStore
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.kriniks.kartun.UpscaleViewModel

private const val MAX_ZOOM = 8f

/**
 * Result (mockup 'result'): before/after over the whole screen. The WHOLE picture is the handle - one finger anywhere
 * moves the divider (the owner's ask from the desktop Upscayl: «ползунок, который можно пальцем схватить»); two fingers
 * zoom and pan both images together. The panel shows the sizes and the time; «Сохранить» / «Поделиться» wait for
 * phase 3 and are shown disabled.
 * The divider: [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md, case C4]. Pinch zoom and pan: [NOT-TESTED] (adb input has no multi-touch).
 */
@Composable
fun ResultScreen(vm: UpscaleViewModel) {
    val src = vm.source ?: return
    val res = vm.result ?: return
    val c = MaterialTheme.colorScheme
    BackHandler { vm.back() }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareTitle = stringResource(R.string.share)
    var saveOpen by remember { mutableStateOf(false) }
    // «Поделиться»: a PNG copy in the cache, then the system share sheet (plans/01 §2.9).
    val share: () -> Unit = {
        scope.launch {
            vm.shareUri()?.let { uri ->
                val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(send, shareTitle))
            }
        }
    }
    var split by remember { mutableFloatStateOf(0.5f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    var pinching = false
                    split = (first.position.x / size.width).coerceIn(0f, 1f)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2) {
                            pinching = true
                            zoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            // Keep the picture covering the screen: pan is bounded by how much the zoom enlarged it.
                            val maxX = size.width * (zoom - 1) / 2
                            val maxY = size.height * (zoom - 1) / 2
                            val p = pan + event.calculatePan()
                            pan = Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
                        } else if (!pinching) {
                            event.changes.firstOrNull { it.pressed }?.let { split = (it.position.x / size.width).coerceIn(0f, 1f) }
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
        ) {
            val layer = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }
            Image(res.before.asImageBitmap(), null, layer, contentScale = ContentScale.Fit)
            // «После» is the same frame clipped to the right of the divider (screen space, so zoom keeps them aligned).
            Box(Modifier.fillMaxSize().drawWithContent { clipRect(left = split * size.width) { this@drawWithContent.drawContent() } }) {
                Image(res.display.asImageBitmap(), null, layer, contentScale = ContentScale.Fit)
            }
            val x = maxWidth * split
            Box(Modifier.offset(x = x - 1.5.dp).width(3.dp).fillMaxHeight().background(c.primary))
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = x - 26.dp).size(52.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(c.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_lr), null, Modifier.size(26.dp), tint = c.onPrimary) }
        }
        // Top bar and the two tags.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                RoundIconButton(R.drawable.ic_back, onClick = vm::back, translucent = true)
                Spacer(Modifier.weight(1f))
                RoundIconButton(R.drawable.ic_share, onClick = share, translucent = true, enabled = !vm.saving)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Pill(stringResource(R.string.before), translucent = true)
                Spacer(Modifier.weight(1f))
                Pill(stringResource(R.string.after), on = true)
            }
        }
        // Result panel (.result-panel).
        Column(
            // widthIn: in landscape the panel stays a compact card at the bottom instead of a full-width strip
            // (the owner: «лендскейп нужно поддерживать тоже»). [NOT-TESTED: landscape - built only; the device run waits for the owner («потом потестируем»)]
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 28.dp)
                .widthIn(max = 600.dp).fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(c.surfaceContainerLow.copy(alpha = 0.94f)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.size_to, sizeText(src.width, src.height), sizeText(res.full.width, res.full.height)), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
                Text(stringResource(R.string.result_time, durationText(res.ms), stringResource(if (res.gpu) R.string.backend_gpu else R.string.backend_cpu)),
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
            }
            vm.saveMessage?.let { Notice(stringResource(it), vm::dismissSaveMessage) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(stringResource(if (vm.saving) R.string.saving else R.string.save), R.drawable.ic_save, onClick = { saveOpen = true },
                    modifier = Modifier.weight(1f), enabled = !vm.saving, height = 54)
                SecondaryButton(stringResource(R.string.share), R.drawable.ic_share, onClick = share, modifier = Modifier.weight(1f),
                    outlined = false, enabled = !vm.saving, height = 54)
            }
        }
    }
    if (saveOpen) SaveDialog(vm, res.full) { saveOpen = false }
}

/**
 * «Сохранить в галерею» (mockup 'save': format chips, the quality slider, the folder line, the pink button). A centred
 * dialog rather than the mockup's sheet - `[AI]` by analogy with the owner's word on the (i) windows («Инфо открывать
 * диалогами, а не ботом шитами»). WebP is off for results over 16 383 px. [NOT-TESTED: device run deferred by the owner]
 */
@Composable
private fun SaveDialog(vm: UpscaleViewModel, full: android.graphics.Bitmap, onClose: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(c.surfaceContainerLow).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.save_title), style = MaterialTheme.typography.titleLarge)
            SectionLabelPlain(stringResource(R.string.label_format))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultStore.Format.entries.forEach { f ->
                    val ok = ResultStore.fits(f, full)
                    Pill(f.name.uppercase(), on = vm.saveFormat == f, modifier = Modifier.alpha(if (ok) 1f else 0.4f),
                        onClick = if (ok) ({ vm.saveFormat = f }) else null)
                }
            }
            if (!ResultStore.fits(ResultStore.Format.Webp, full)) {
                Text(stringResource(R.string.webp_too_big), style = MaterialTheme.typography.bodySmall, color = c.error)
            }
            if (vm.saveFormat.hasQuality) {
                SectionLabelPlain(stringResource(R.string.label_quality, vm.saveQuality))
                Slider(
                    value = vm.saveQuality.toFloat(), onValueChange = { vm.saveQuality = it.roundToInt() }, valueRange = 50f..100f,
                    colors = SliderDefaults.colors(thumbColor = c.onBackground, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceContainerHigh),
                )
            }
            Text(stringResource(R.string.save_folder), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            PrimaryButton(stringResource(R.string.save), R.drawable.ic_save, onClick = { vm.save(); onClose() })
        }
    }
}

/** A pink upper-case label without the (i) button (the save dialog's own settings need no explanation window). */
@Composable
private fun SectionLabelPlain(text: String) =
    Text(text.uppercase(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
