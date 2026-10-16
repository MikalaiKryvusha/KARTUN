package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.animation.core.animate
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import android.content.Intent
import com.kriniks.kartun.UpscaleViewModel

private const val MAX_ZOOM = 8f
/** Double tap on the picture zooms to this and back (the same gesture as the «Улучшение» preview, plan 03 step 17). */
private const val TAP_ZOOM = 2.5f
/** How close to the divider a finger must land to grab it, each side. */
private val DIVIDER_GRAB = 36.dp
/** An invisible round grab zone around the pink knob, wider than the knob itself (52 dp): the owner, chat 2026-10-14
 *  ≈ 23:05 +03:00: «вокруг драга до после нужно сделать невидимую более широкую область, за которую его можно хватать». */
private val KNOB_GRAB = 72.dp
/** How long the divider fades in at the top and out at the bottom: short and sharp (the owner, chat 2026-10-15 ≈ 11:36
 *  +03:00: «фейды полоски До-После короче и резче, сейчас длинные очень - некрасиво»; was 12 % of the line, ≈ 96 dp). */
private val DIVIDER_FADE = 24.dp
/** The divider's gap from the status bar above and the navigation bar below. */
private val DIVIDER_GAP = 8.dp
/** A released panel hides when dragged past this share of the screen width (or flung right faster than PANEL_FLING px/s). */
private const val PANEL_HIDE_AT = 0.25f
private const val PANEL_FLING = 1200f

/**
 * Result (mockup 'result'): before/after over the whole screen. The divider moves only when the finger lands ON it (the
 * line or its pink knob, ±DIVIDER_GRAB); a finger anywhere else grabs the PHOTO and moves it (the owner, chat 2026-10-14
 * ≈ 21:00 +03:00: «перемешает слайдер до после - а должен хватать фото на навигацию»); a double tap zooms ×TAP_ZOOM at
 * that point and back; two fingers zoom and pan both images together. Divider vs photo grab and the KNOB_GRAB zone:
 * [TESTED: 2026-10-15 · Titan, adb swipes read from screencaps - 55 dp off the line far above the knob leaves it at 800 px,
 * 55 dp off at the knob's height drags it to 1301 px; testcases/reports/2026-10-15_F2_guarded_loop_device_run.md].
 * The panel shows the model, the sizes and the time, «Сохранить» and «Поделиться».
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
                // ClipData + title: the stock Android 16 share sheet shows the file's name and a preview only from them - with
                // EXTRA_STREAM alone it said just «Отправка изображения» (RC smoke on the Titan, 2026-10-16). [NOT-TESTED]
                val name = uri.lastPathSegment.orEmpty()
                val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TITLE, name)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                send.clipData = ClipData.newUri(ctx.contentResolver, name, uri)
                ctx.startActivity(Intent.createChooser(send, shareTitle))
            }
        }
    }
    var split by remember { mutableFloatStateOf(0.5f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    // The panel slides off the right edge (the owner, chat 2026-10-14 ≈ 21:08 +03:00: «виджет сводки на экране результата
    // сделаем скрывающимся в сторону за пределы экрана, и чтобы на краю оставалась стрелочка, в которую он скрылся. И оттуда
    // его можно обратно раскрыть»): a sideways swipe on the panel hides it, the arrow tab at the edge (tap or swipe) brings it back.
    var screenW by remember { mutableFloatStateOf(0f) }
    var panelH by remember { mutableFloatStateOf(0f) }
    // px to the right: 0 = shown, screenW = hidden. The finger moves it SYNCHRONOUSLY; only the release animates. The first
    // version launched a coroutine per drag delta, a late one cancelled the release animation or fed it a stale value - the
    // owner saw the panel stop half way (chat 2026-10-14 ≈ 22:51 +03:00: «виджет сводки зависает на половине»); a 500 px drag
    // on the Titan came back instead of hiding.
    var panelShift by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    // «Hidden» flips only when a release animation ENDS - never in the middle of a gesture (see the tab below).
    var panelHidden by remember { mutableStateOf(false) }
    fun settle(show: Boolean) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(panelShift, if (show) 0f else screenW) { v, _ -> panelShift = v }
            panelHidden = !show
        }
    }
    val panelDrag = rememberDraggableState { d -> panelShift = (panelShift + d).coerceIn(0f, screenW) }
    // The knob's height on the line: halfway between the screen's top and the panel's top edge while the panel is shown -
    // the centre of the photo that is actually visible; it slides to the screen's centre as the panel is swiped away (the
    // owner, chat 2026-10-15 ≈ 11:45 +03:00: «кружок-ручка До-После определяла свое положение на линии по высоте ОТ верхнего
    // края виджета-сводки результата и ДО верха экрана» · «А когда виджет всторону сммахиваешь - чтобы смещалось в центр
    // экрана, как сейчас»). Read by both the drawing and the grab test, so the grab zone travels with the knob.
    var screenH by remember { mutableFloatStateOf(0f) }
    var panelTop by remember { mutableFloatStateOf(0f) }
    fun knobY(): Float {
        val centre = screenH / 2f
        if (screenW <= 0f || panelTop <= 0f) return centre
        val shown = 1f - (panelShift / screenW).coerceIn(0f, 1f)
        return centre + (panelTop / 2f - centre) * shown
    }
    // The height of the visible picture area: from the screen's top to twice the knob's height (its centre is knobY()).
    fun visibleH(): Float = (knobY() * 2f).coerceAtLeast(1f)
    val stopSettle: suspend CoroutineScope.(Offset) -> Unit = { settleJob?.cancel() }

    Box(Modifier.fillMaxSize().background(c.background).onSizeChanged { screenW = it.width.toFloat(); screenH = it.height.toFloat() }) {
        BoxWithConstraints(
            Modifier.fillMaxSize().pointerInput(Unit) {
                // Keep the picture covering what it covered at 1×: pan is bounded by how much the zoom enlarged it. The
                // picture lives in the visible area above the summary panel (visibleH), centred at knobY().
                fun bounded(p: Offset): Offset {
                    val maxX = size.width * (zoom - 1) / 2
                    val maxY = visibleH() * (zoom - 1) / 2
                    return Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
                }
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val knob = Offset(split * size.width, knobY())
                    val onDivider = kotlin.math.abs(first.position.x - split * size.width) <= DIVIDER_GRAB.toPx() ||
                        (first.position - knob).getDistance() <= KNOB_GRAB.toPx()
                    var pinching = false
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2) {
                            pinching = true
                            // About the point between the fingers (Pinch.kt; the owner 2026-10-16: «центр тяжести … в центре фото»).
                            val (z, p) = pinchStep(zoom, pan, event.calculateCentroid(useCurrent = false), Offset(size.width / 2f, knobY()), event.calculateZoom(), event.calculatePan(), MAX_ZOOM)
                            zoom = z
                            pan = bounded(p)
                        } else if (!pinching) {
                            val f = event.changes.firstOrNull { it.pressed }
                            if (f != null) {
                                if (onDivider) split = (f.position.x / size.width).coerceIn(0f, 1f)
                                else pan = bounded(pan + (f.position - f.previousPosition))
                            }
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }.pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { at ->
                    if (zoom > 1f) { zoom = 1f; pan = Offset.Zero }
                    else {
                        // Zoom in about the tapped point: that point stays under the finger.
                        zoom = TAP_ZOOM
                        val centre = Offset(size.width / 2f, knobY())
                        val maxX = size.width * (zoom - 1) / 2
                        val maxY = visibleH() * (zoom - 1) / 2
                        val p = (centre - at) * (zoom - 1)
                        pan = Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
                    }
                })
            },
        ) {
            // The picture is fitted into the area between the screen's top and the summary panel's top edge, so it stands in
            // the middle of what is visible; as the panel is swiped away the area grows to the whole screen (the owner, chat
            // 2026-10-16 ≈ 15:30: «нужно в центр между верхом экрана и верхней границей нижнего виджета сводки»). [NOT-TESTED]
            val visibleDp = with(LocalDensity.current) { visibleH().toDp() }
            val layer = Modifier.fillMaxWidth().height(visibleDp).graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }
            // Named for TalkBack: the photo before and after (the owner ≈ 17:43: «Все им должно быть описано»).
            Image(res.before.asImageBitmap(), stringResource(R.string.before), layer, contentScale = ContentScale.Fit)
            // «После» is the same frame clipped to the right of the divider (screen space, so zoom keeps them aligned).
            Box(Modifier.fillMaxSize().drawWithContent { clipRect(left = split * size.width) { this@drawWithContent.drawContent() } }) {
                Image(res.display.asImageBitmap(), stringResource(R.string.after), layer, contentScale = ContentScale.Fit)
            }
            val x = maxWidth * split
            // The divider runs the screen's height inside the system bars with a small gap from each, and fades to
            // transparent at both ends (the owner, chat 2026-10-14 ≈ 21:05 +03:00: «перекрывает вверху статус бар ОС. Нужно
            // её начинать мягкий фейдом и заканчивать мягим фейдом»; ≈ 21:11: «слишком сократил полосу до после. Пусть от
            // верха девайся буде до низа, но немножко от низа отсутпает, и немножко от верха, чтобы статус бар не перекрывала»).
            val bars = WindowInsets.systemBars.asPaddingValues()
            val lineTop = bars.calculateTopPadding() + DIVIDER_GAP
            val lineBottom = maxHeight - bars.calculateBottomPadding() - DIVIDER_GAP
            val fade = (DIVIDER_FADE / (lineBottom - lineTop)).coerceIn(0f, 0.5f)
            Box(
                Modifier.offset(x = x - 1.5.dp, y = lineTop).width(3.dp).height(lineBottom - lineTop).background(
                    Brush.verticalGradient(0f to Color.Transparent, fade to c.primary, 1f - fade to c.primary, 1f to Color.Transparent),
                ),
            )
            Box(
                Modifier.offset { IntOffset((x - 26.dp).roundToPx(), (knobY() - 26.dp.toPx()).roundToInt()) }.size(52.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(c.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_lr), stringResource(R.string.compare_handle), Modifier.size(26.dp), tint = c.onPrimary) }
        }
        // «Рендер…» over the before/after picture while the file for «Сохранить» / «Поделиться» is written - before it the
        // screen only greyed out and nothing said that work was going on (the owner, chat 2026-10-15 ≈ 11:38 +03:00:
        // «нужно крутить спиннер "Рендер"…» · «или не в виджене крутить, а поверх канваса результата До-После»).
        if (vm.saving) {
            Column(
                // Opaque: at 90 % the pink knob and the line of the divider showed through the card (Titan 11:43).
                Modifier.align(Alignment.Center).shadow(10.dp, MaterialTheme.shapes.extraLarge).clip(MaterialTheme.shapes.extraLarge)
                    .background(c.surfaceContainerLow).padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PixelSpinner(Modifier.size(48.dp))
                Text(stringResource(R.string.rendering), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                // Small «Отменить»: stops a long render and deletes the half-written file (the owner, chat 2026-10-15 ≈ 11:53).
                // A fixed width: SecondaryButton fills its parent, and the card stretched over the whole screen (the
                // owner, chat 2026-10-15 ≈ 12:31 +03:00: «диалог Рендер некрасиво растянуло на весь экран у тебя»).
                SecondaryButton(stringResource(R.string.cancel), null, onClick = vm::cancelRender, height = 36, modifier = Modifier.width(COMPACT_BUTTON))
            }
        }
        // Top bar and the two tags.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                // Back only: «Поделиться» lives once, in the panel below (the owner, chat 2026-10-14 ≈ 20:54 +03:00:
                // «непонятно зачем две кнопки поделиться на результате»).
                RoundIconButton(R.drawable.ic_back, onClick = vm::back, translucent = true)
            }
            // The picture is not mirrored in right-to-left languages (the «before» stays left of the divider), so its tags
            // keep the same sides: in Arabic «بعد» stood over the original (RC smoke on the S21 FE, 2026-10-16). [NOT-TESTED]
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                // Both tags quiet and see-through (the owner, chat 2026-10-14 ≈ 20:58 +03:00: «до после можно не ярко, а
                // полупрозрачно» · «после сейчас слишком кричащее»); the pink stays on the divider and the buttons.
                Pill(stringResource(R.string.before), translucent = true)
                Spacer(Modifier.weight(1f))
                Pill(stringResource(R.string.after), translucent = true)
            } }
        }
        // Result panel (.result-panel).
        Column(
            // widthIn: in landscape the panel stays a compact card at the bottom instead of a full-width strip
            // (the owner: «лендскейп нужно поддерживать тоже»). [NOT-TESTED: landscape - built only; the device run waits for the owner («потом потестируем»)]
            // bottom 8 dp, was 28 (the owner, chat 2026-10-14 ≈ 21:01 +03:00: «виджет сводки чуть ниже сместить»).
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .offset { IntOffset(panelShift.roundToInt(), 0) }.onSizeChanged { panelH = it.height.toFloat() }
                .onGloballyPositioned { panelTop = it.positionInParent().y }
                .draggable(panelDrag, Orientation.Horizontal, onDragStarted = stopSettle, onDragStopped = { v ->
                    settle(show = panelShift < screenW * PANEL_HIDE_AT && v < PANEL_FLING)
                })
                .widthIn(max = 600.dp).fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(c.surfaceContainerLow).padding(16.dp),  // opaque: at 94 % the divider showed through (RC2 check, Titan)
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Which model made it (the owner, chat 2026-10-14 ≈ 20:59 +03:00: «в сводке внизу нужно писать, какая модель улучшила»).
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(res.model.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                // The visible way to hide the panel - a swipe alone is undiscoverable (the owner, chat 2026-10-14 ≈ 21:13
                // +03:00: «что виджет сводки можно смахнуть - никто не догадается, ему нужен или драг хендл или кнопочка-стрелочка»).
                RoundIconButton(R.drawable.ic_back, onClick = { settle(show = false) }, modifier = Modifier.rotate(180f))
            }
            // The sizes never break inside («960 × 1280 → / 3840 × 5120» on a phone - the owner, 2026-10-16 ≈ 20:4x: «Чинить»):
            // each text stays whole on its line, and when both do not fit side by side the time moves under the sizes. [NOT-TESTED]
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SizeChange(sizeText(src.width, src.height), sizeText(res.full.width, res.full.height), MaterialTheme.typography.bodyMedium, c.onBackground,
                    Modifier.padding(end = 12.dp))  // a gap to the time when both share a line: in Arabic they ran together (RC2, Redmi)
                Text(stringResource(R.string.result_time, durationText(res.ms), stringResource(if (res.gpu) R.string.backend_gpu else R.string.backend_cpu)),
                    style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, softWrap = false, maxLines = 1)
            }
            vm.saveMessage?.let { Notice(stringResource(it), vm::dismissSaveMessage, success = it == R.string.saved) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(stringResource(if (vm.saving) R.string.saving else R.string.save), R.drawable.ic_save, onClick = { saveOpen = true },
                    modifier = Modifier.weight(1f), enabled = !vm.saving, height = 54)
                SecondaryButton(stringResource(R.string.share), R.drawable.ic_share, onClick = share, modifier = Modifier.weight(1f),
                    outlined = false, enabled = !vm.saving, height = 54)
            }
        }
        // The arrow tab the panel hid into: on the right edge at the panel's height; tap or swipe left brings it back.
        // ALWAYS composed, faded by the panel's position: it used to leave the composition once the pulled panel passed
        // the middle, its drag died under the finger with no release, and the panel stayed half out (the owner, chat
        // 2026-10-14 ≈ 22:53 +03:00: «когда он спрятан, и тянешь свайпом его» · «то он на половине становится» · «не
        // вытягивается полностью»). Its gestures switch on and off only when a release animation ends (panelHidden).
        run {
            // Smaller since the owner's word (chat 2026-10-14 ≈ 22:52 +03:00: «в закрытом состоянии меньше стрелку»): 28 x 56 dp.
            val tabH = 56.dp
            Box(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                    // Never below zero: before the panel is measured panelH is 0 and the sum is -20 dp - Compose throws
                    // «Padding must be non-negative» (crash on the Titan 2026-10-14 23:22:42, the owner: «креш»).
                    .padding(bottom = (8.dp + with(LocalDensity.current) { (panelH / 2).toDp() } - tabH / 2).coerceAtLeast(0.dp))
                    .size(width = 28.dp, height = tabH)
                    // The tab sits on the edge where Android's gesture navigation reads a swipe as «Back» (seen on the
                    // Titan 21:11: a swipe left on the tab left the screen); the standard opt-out for this strip.
                    .systemGestureExclusion()
                    .alpha(if (screenW > 0f) ((panelShift / screenW - 0.5f) * 2f).coerceIn(0f, 1f) else 0f)
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(c.surfaceContainerHigh.copy(alpha = 0.85f))
                    .clickable(enabled = panelHidden) { settle(show = true) }
                    .draggable(panelDrag, Orientation.Horizontal, enabled = panelHidden, onDragStarted = stopSettle, onDragStopped = { v ->
                        settle(show = panelShift < screenW * (1 - PANEL_HIDE_AT))
                    }),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.panel_show), Modifier.size(16.dp), tint = c.onBackground) }
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
                    Pill(f.name.uppercase(), on = vm.saveFormat == f, onClick = { vm.saveFormat = f })
                }
            }
            // Over the chosen format's side limit: say so and offer the reduced size (the owner, 2026-10-16 ≈ 15:14: «нужно
            // предупреждать в диалоге сохранения, и предложить выполнить сжатие до допустимого размера»). [NOT-TESTED]
            val (fitW, fitH) = ResultStore.fitSize(vm.saveFormat, full.width, full.height)
            val reduced = fitW != full.width || fitH != full.height
            if (reduced) {
                val n = java.text.NumberFormat.getIntegerInstance()
                Text(
                    stringResource(R.string.save_too_big, vm.saveFormat.name.uppercase(), n.format(ResultStore.maxSide(vm.saveFormat)), n.format(fitW), n.format(fitH)),
                    style = MaterialTheme.typography.bodySmall, color = c.error,
                )
            }
            // What the chosen format is for, as three short points (the owner, chat 2026-10-15 ≈ 12:01 +03:00: «нужно писать
            // краткое описания преимуществ выбранного формата, булет-дот списком»). `[AI]` wording, awaiting the owner's read.
            Text(
                stringResource(when (vm.saveFormat) {
                    ResultStore.Format.Png -> R.string.format_points_png
                    ResultStore.Format.Jpeg -> R.string.format_points_jpeg
                    ResultStore.Format.Webp -> R.string.format_points_webp
                }),
                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
            )
            if (vm.saveFormat.hasQuality) {
                SectionLabelPlain(stringResource(R.string.label_quality, vm.saveQuality))
                Slider(
                    value = vm.saveQuality.toFloat(), onValueChange = { vm.saveQuality = it.roundToInt() }, valueRange = 50f..100f,
                    colors = SliderDefaults.colors(thumbColor = c.onBackground, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceContainerHigh),
                )
            }
            // The file's size, estimated from the picture in the chosen format and quality (ResultStore.estimateBytes);
            // recomputed a moment after the format or the quality stops changing.
            var estimate by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(vm.saveFormat, vm.saveQuality) {
                estimate = null
                kotlinx.coroutines.delay(ESTIMATE_DEBOUNCE_MS)
                // A reduced save holds fewer points: the estimate scales by the area that is actually written.
                val area = fitW.toDouble() * fitH / (full.width.toDouble() * full.height)
                estimate = withContext(Dispatchers.Default) { (ResultStore.estimateBytes(full, vm.saveFormat, vm.saveQuality) * area).toLong() }
            }
            Text(
                boldDigits(stringResource(R.string.save_estimate, estimate?.let { mbText(it) } ?: "…")),
                style = MaterialTheme.typography.bodyMedium, color = c.onBackground,
            )
            Text(stringResource(R.string.save_folder), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            PrimaryButton(stringResource(if (reduced) R.string.save_reduced else R.string.save), R.drawable.ic_save, onClick = { vm.save(); onClose() })
        }
    }
}

/** The save dialog waits this long after the last format/quality change before estimating the size. */
private const val ESTIMATE_DEBOUNCE_MS = 200L

/** Megabytes with one decimal in the user's locale («8,1»). */
private fun mbText(bytes: Long): String = String.format(java.util.Locale.getDefault(), "%.1f", bytes / 1_000_000.0)

/** A pink upper-case label without the (i) button (the save dialog's own settings need no explanation window). */
@Composable
private fun SectionLabelPlain(text: String) =
    Text(text.uppercase(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
