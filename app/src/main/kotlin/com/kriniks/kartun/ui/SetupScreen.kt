package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import android.content.res.Configuration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R
import com.kriniks.kartun.UpscaleViewModel
import kotlin.math.roundToInt

/** Preview height at rest, and the height at which it becomes the small row ('A-setup-scrolled'). */
private val HERO_MAX = 330.dp
private val HERO_MIN = 80.dp
/** How far past the full preview a downward pull must stretch it to open the photo full screen (plans/03 step 9). */
private val PULL_OPEN = 90.dp
/** The stretch never grows beyond this - the rubber band of the pull. */
private val PULL_MAX = 160.dp
/** Zoom of a double tap inside the preview, and of a hold on the un-zoomed photo (room to move it). */
private const val PREVIEW_ZOOM = 2.5f
private const val PREVIEW_HOLD_ZOOM = 1.6f

/**
 * «Фото выбрано» (mockups 'A-setup' / 'A-setup-scrolled', interview #002 Q1): the preview stays on top and shrinks while
 * the settings scroll, becoming a small row past 190 dp; model cards, the 1×–16× scale slider, the pinned «Улучшить»
 * button with the time estimate (interview #003). Format/quality/extra settings come with phase 3. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
@Composable
fun SetupScreen(vm: UpscaleViewModel, onPickOther: () -> Unit) {
    val src = vm.source ?: return
    val c = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var info by remember { mutableStateOf<Int?>(null) }
    // Collapsing header (the standard pattern): an upward swipe first shrinks the preview, and only a fully collapsed
    // preview lets the settings scroll; a downward swipe first scrolls the settings back to the top, then grows the
    // preview. The settings' viewport never changes height under the finger - the jump the owner saw
    // («прыгает картинка на экране Улучшение», 2026-10-14) came from shrinking the preview BY the scroll offset.
    val maxPx = with(density) { HERO_MAX.toPx() }
    val minPx = with(density) { HERO_MIN.toPx() }
    var heroPx by remember { mutableFloatStateOf(maxPx) }
    // Pull-down past the full preview: the photo stretches like a rubber band; released past PULL_OPEN it opens full
    // screen - the owner in chat 2026-10-14 ≈ 18:57: «свайпом вниз было бы здорово раскрывать вью картинки на весь экран».
    var pullPx by remember { mutableFloatStateOf(0f) }
    var fullscreen by remember { mutableStateOf(false) }
    val openPx = with(density) { PULL_OPEN.toPx() }
    val pullMaxPx = with(density) { PULL_MAX.toPx() }
    val collapse = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0) return Offset.Zero
                // An upward move first takes back the stretch, then shrinks the preview.
                var left = available.y
                if (pullPx > 0f) {
                    val p = (pullPx + left).coerceAtLeast(0f)
                    left -= p - pullPx
                    pullPx = p
                }
                val next = (heroPx + left).coerceAtLeast(minPx)
                left -= next - heroPx
                heroPx = next
                return Offset(0f, available.y - left)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0) return Offset.Zero
                val next = (heroPx + available.y).coerceAtMost(maxPx)
                var left = available.y - (next - heroPx)
                heroPx = next
                // Only a finger drag stretches (a fling arriving at the top must not open the viewer).
                if (left > 0f && source == NestedScrollSource.UserInput) {
                    pullPx = (pullPx + left * 0.5f).coerceAtMost(pullMaxPx)
                    left = 0f
                }
                return Offset(0f, available.y - left)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullPx >= openPx) fullscreen = true
                pullPx = 0f
                return Velocity.Zero
            }
        }
    }
    val heroDp = with(density) { (heroPx + pullPx).toDp() }
    BackHandler { vm.back() }

    // Landscape (the owner, 2026-10-14 ≈ 19:12: «лендскейп нужно поддерживать тоже»): the photo on the left at full height,
    // the settings scrolling on the right with the button pinned under them; no collapsing header - the photo has its own
    // half, a tap opens it full screen. [NOT-TESTED: landscape - built only; the device run waits for the owner («потом потестируем»)]
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Box(Modifier.fillMaxSize().background(c.background)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                SetupTopBar(vm)
                Row(Modifier.weight(1f).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PhotoCard(src, onOpen = { fullscreen = true }, onPickOther = onPickOther,
                        modifier = Modifier.weight(1f).fillMaxHeight().navigationBarsPadding().padding(bottom = 20.dp))
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 130.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) { SettingsBlock(vm) { info = it } }
                        UpscaleButton(vm, Modifier.align(Alignment.BottomCenter), sidePadding = 0.dp)
                    }
                }
            }
        }
        SetupOverlays(src.bitmap, fullscreen, { fullscreen = false }, info, { info = null }, vm::openManual)
        return
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().nestedScroll(collapse)) {
            SetupTopBar(vm)
            // Pinned preview: the photo card shrinking with the swipe; at its floor it is the compact row.
            // The photo (card or compact row) is a handle too: dragging it feeds the same collapse/stretch as the settings do.
            val pull = rememberDraggableState { d ->
                val delta = Offset(0f, d)
                if (d < 0) collapse.onPreScroll(delta, NestedScrollSource.UserInput)
                else collapse.onPostScroll(Offset.Zero, delta, NestedScrollSource.UserInput)
            }
            val drag = Modifier.draggable(pull, Orientation.Vertical, onDragStopped = { collapse.onPreFling(Velocity.Zero) })
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp)) {
                if (heroDp <= HERO_MIN + 0.5.dp) {
                    Row(
                        Modifier.fillMaxWidth().height(HERO_MIN).then(drag).clip(MaterialTheme.shapes.medium).background(c.surfaceContainer).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(src.bitmap.asImageBitmap(), null, Modifier.size(64.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text(src.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(boldDigits(stringResource(R.string.size_to, sizeText(src.width, src.height), sizeText(src.width * vm.scale, src.height * vm.scale))),
                                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        }
                        Pill(stringResource(R.string.setup_other_short), outlined = true, onClick = onPickOther)
                    }
                } else {
                    PhotoCard(src, onOpen = { fullscreen = true }, onPickOther = onPickOther, modifier = Modifier.fillMaxWidth().height(heroDp).then(drag))
                }
                // The handle stays under the photo in BOTH states (the owner, chat 2026-10-14 ≈ 20:57 +03:00: «когда картинку ты
                // свернул - драг хендл пропал - баг»): collapsed, it is what tells that the row pulls back down.
                DragHandle(drag)
            }
            Column(
                Modifier.weight(1f).verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsBlock(vm) { info = it }
            }
        }
        UpscaleButton(vm, Modifier.align(Alignment.BottomCenter))
    }
    SetupOverlays(src.bitmap, fullscreen, { fullscreen = false }, info, { info = null }, vm::openManual)
}

/** Back button and the screen title (the same in both orientations). */
@Composable
private fun SetupTopBar(vm: UpscaleViewModel) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(R.drawable.ic_back, onClick = vm::back)
        Text(stringResource(R.string.setup_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        // No gear here: the settings open from the home screen only (the owner, chat 2026-10-14 ≈ 20:50 +03:00:
        // «в настройки пускать только с главного экрана приложения»); an empty box of the button's size keeps the title centred.
        Spacer(Modifier.size(44.dp))
    }
}

/** The photo card: the picked photo cropped to the card, its size chip and «Выбрать другое фото»; a tap opens it full screen. */
@Composable
private fun PhotoCard(src: UpscaleViewModel.Source, onOpen: () -> Unit, onPickOther: () -> Unit, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    // Inside the preview (the owner, chat 2026-10-14 ≈ 20:16: «превью фото можно тапом и удержанием двигать и перемещать
    // прямо во вьюшке превью» · «даблтап увеличивает»): a double tap zooms ×PREVIEW_ZOOM at the tapped point and back; a
    // long press then a drag moves the photo inside the card; a single tap opens it full screen. A plain drag (no hold)
    // stays the collapse/expand handle of the card - the hold is what tells the two apart.
    // [NOT-TESTED: built 2026-10-14 ≈ 20:16, installed on the Titan, not walked - the next chat starts with it]
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier.clip(MaterialTheme.shapes.large).background(c.surfaceContainer)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onOpen() },
                    onDoubleTap = { at ->
                        if (zoom > 1f) { zoom = 1f; pan = Offset.Zero }
                        else {
                            zoom = PREVIEW_ZOOM
                            // keep the tapped point under the finger: move the centre towards it
                            val centre = Offset(size.width / 2f, size.height / 2f)
                            pan = clampPan((centre - at) * (PREVIEW_ZOOM - 1f), zoom, size.width.toFloat(), size.height.toFloat())
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress { change, drag ->
                    change.consume()
                    if (zoom == 1f) zoom = PREVIEW_HOLD_ZOOM // a hold on the un-zoomed photo zooms a little so there is room to move
                    pan = clampPan(pan + drag, zoom, size.width.toFloat(), size.height.toFloat())
                }
            },
    ) {
        Image(
            src.bitmap.asImageBitmap(), null,
            Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y },
            contentScale = ContentScale.Crop,
        )
        Pill("${sizeText(src.width, src.height)} · ${stringResource(R.string.megapixels, mpText(src.megapixels))}",
            Modifier.align(Alignment.BottomStart).padding(12.dp), translucent = true, heavyDigits = true)
        Row(
            Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(c.background.copy(alpha = 0.72f))
                .clickable(onClick = onPickOther).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_image), null, Modifier.size(16.dp), tint = c.onBackground)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.setup_other_photo), style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize))
        }
    }
}

/** Keeps the zoomed photo covering the card: the shift is bounded by how much the zoom enlarged it. */
private fun clampPan(p: Offset, zoom: Float, w: Float, h: Float): Offset {
    val mx = w * (zoom - 1f) / 2f
    val my = h * (zoom - 1f) / 2f
    return Offset(p.x.coerceIn(-mx, mx), p.y.coerceIn(-my, my))
}

/** The drag handle under the preview (the owner: «под превью фото нужно добавить драг-хендл»): the grab bar of an
 *  Android sheet - it tells that the photo can be pulled - and it pulls exactly like the photo does. */
@Composable
private fun DragHandle(modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 40.dp, height = 5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

/** The settings: the error notice, the model cards and the scale slider, each section with its (i). */
@Composable
private fun SettingsBlock(vm: UpscaleViewModel, onInfo: (Int) -> Unit) {
    vm.error?.let { (res, arg) -> Notice(if (arg != null) stringResource(res, arg) else stringResource(res), vm::dismissError) }
    SectionLabel(stringResource(R.string.label_model)) { onInfo(R.string.label_model) }
    vm.modelOrder.forEach { m -> ModelCard(m, m == vm.model, m == vm.model && vm.modelLoading, durationText(vm.estimateMs(m))) { vm.chooseModel(m) } }
    SectionLabel(stringResource(R.string.label_scale)) { onInfo(R.string.label_scale) }
    ScaleSlider(vm)
}

/** «Улучшить · ≈ T» pinned at the bottom over a fade (.cta). */
@Composable
private fun UpscaleButton(vm: UpscaleViewModel, modifier: Modifier, sidePadding: androidx.compose.ui.unit.Dp = 20.dp) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().background(Brush.verticalGradient(fadeColors(c.background)))
            .navigationBarsPadding().padding(start = sidePadding, end = sidePadding, top = 40.dp, bottom = 24.dp),
    ) {
        PrimaryButton(
            stringResource(R.string.upscale_with_estimate, durationText(vm.estimateMs())), R.drawable.ic_spark,
            // [OWNER] interview #004, Q1 = A: the notification permission is asked at the first «Улучшить».
            onClick = rememberStartWithNotificationAsk(vm::start), enabled = vm.scaleSupported,
        )
    }
}

/** The full-screen photo and the (i) dialogs, over either layout. */
@Composable
private fun SetupOverlays(bitmap: android.graphics.Bitmap, fullscreen: Boolean, closeFull: () -> Unit, info: Int?, closeInfo: () -> Unit, openGuide: () -> Unit) {
    if (fullscreen) FullPhoto(bitmap, closeFull)
    when (info) {
        R.string.label_scale -> InfoDialog(stringResource(R.string.label_scale), stringResource(R.string.info_scale_text), stringResource(R.string.info_scale_list), onGuide = { closeInfo(); openGuide() }, onClose = closeInfo)
        R.string.label_model -> InfoDialog(stringResource(R.string.label_model), stringResource(R.string.info_model_text), stringResource(R.string.info_model_list), onGuide = { closeInfo(); openGuide() }, onClose = closeInfo)
    }
}

/** A model card (.mrow): name, what it is for with the estimate, the licence badge; pink border + tick when chosen. */
@Composable
private fun ModelCard(m: UpscaleViewModel.Model, on: Boolean, loading: Boolean, estimate: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (on) Brush.horizontalGradient(listOf(c.primaryContainer, c.surfaceContainer)) else Brush.linearGradient(listOf(c.surfaceContainer, c.surfaceContainer)))
            .border(2.dp, if (on) c.primary else c.surfaceContainer, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(m.title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.model_desc_with_estimate, stringResource(m.desc), estimate), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            Text(
                stringResource(R.string.licence_bsd),
                Modifier.clip(CircleShape).background(c.tertiary.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.bodySmall.letterSpacing),
                color = c.tertiary,
            )
        }
        // While the chosen model loads (preload on this screen) a small pink ring stands where the tick will be.
        if (on && loading) CalmSpinner(Modifier.size(22.dp), strokeWidth = 2.5.dp)
        else if (on) Box(Modifier.size(22.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_tick), null, Modifier.size(14.dp), tint = c.onPrimary)
        }
    }
}

/**
 * Scale slider: the native Material 3 slider with five steps 1×, 2× (default), 4×, 8×, 16×, the step marks (the chosen one lit), the resulting
 * size and megapixels; 8× and 16× show the notice that they come with phase 1. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
@Composable
private fun ScaleSlider(vm: UpscaleViewModel) {
    val src = vm.source ?: return
    val c = MaterialTheme.colorScheme
    val last = UpscaleViewModel.SCALES.lastIndex
    Column(Modifier.padding(horizontal = 6.dp)) {
        // The native Material 3 slider, styled like the quality slider of the save dialog (the owner, chat 2026-10-14
        // ≈ 22:41 +03:00: «как ползунок качества сделал - по дизайну современного андроид. Так и сделай в таком стиле выбор
        // масштаба» · «нативным элементом ползунка»): five positions, the chosen one lit in the marks below.
        Slider(
            value = vm.scaleIndex.toFloat(), onValueChange = { vm.scaleIndex = it.roundToInt().coerceIn(0, last) },
            valueRange = 0f..last.toFloat(), steps = last - 1,
            colors = SliderDefaults.colors(thumbColor = c.onBackground, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceContainerHigh),
        )
        // Step marks at 0, 25, 50, 75, 100 % - the first left-aligned, the last right-aligned, the rest centred.
        Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            UpscaleViewModel.SCALES.forEachIndexed { i, s ->
                // Plain «2×», no «по умолч.» (the owner, chat 2026-10-14 ≈ 20:49 +03:00: «убрать строчку По умолч»).
                Text(
                    "$s×",
                    Modifier.layout { measurable, constraints ->
                        val pl = measurable.measure(constraints.copy(minWidth = 0))
                        val at = constraints.maxWidth * i / last
                        val left = when (i) { 0 -> 0; last -> at - pl.width; else -> at - pl.width / 2 }
                        layout(constraints.maxWidth, pl.height) { pl.place(left, 0) }
                    },
                    style = if (i == vm.scaleIndex) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    color = if (i == vm.scaleIndex) c.primary else c.onSurfaceVariant,
                )
            }
        }
        val w = src.width * vm.scale
        val h = src.height * vm.scale
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(boldDigits(stringResource(R.string.size_to, sizeText(src.width, src.height), sizeText(w, h))), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
            Text(stringResource(R.string.megapixels, mpText(w.toFloat() * h / 1_000_000f)), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        // 8× and 16× run two passes (plans/04 step 1): the mockup's amber «2 прохода · дольше · большой файл»; a result over
        // the output limit is refused with the largest scale that fits this photo.
        val notice = when {
            !vm.scaleSupported -> stringResource(R.string.scale_too_big, UpscaleViewModel.MAX_OUTPUT_MP.toInt().toString(), vm.largestFittingScale)
            vm.scale > UpscaleViewModel.ENGINE_SCALE -> stringResource(R.string.scale_two_pass)
            else -> null
        }
        if (notice != null) {
            Text(
                notice,
                Modifier.padding(top = 10.dp).clip(CircleShape).background(c.error.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = c.error,
            )
        }
    }
}
