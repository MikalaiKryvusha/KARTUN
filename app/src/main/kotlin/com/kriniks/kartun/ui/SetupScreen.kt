package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import android.content.res.Configuration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kriniks.kartun.R
import com.kriniks.kartun.Model
import com.kriniks.kartun.UpscaleViewModel
import kotlin.math.roundToInt

/** How far a downward scroll of the settings grows the preview back, and the height at which it becomes the small row
 *  ('A-setup-scrolled'). The handle itself grows the preview past HERO_MAX, as far as it is pulled. */
private val HERO_MAX = 330.dp
private val HERO_MIN = 80.dp
/** The handle released below this share of the screen height opens the photo full screen (the owner, 2026-10-16 ≈ 12:46:
 *  «на всеь экран пусть разворачивается, когда драг хендл ниже, чем 80% высоты экрана»). */
private const val FULL_SCREEN_AT = 0.8f
/** Hysteresis between the row and the card (the owner, 2026-10-16 ≈ 12:53: «разворот фото из строки во вьюшку слишком
 *  быстры, без гистерезиса»): the row stretches with the handle and turns into the card only this far past its own height;
 *  the card turns back into the row only this close to it; ≈ 12:55: «нужно добавить там чуть-чуть тоже гистерезиса в
 *  начале, чтобы микро подвижки туда сюда не переключали из строки во вью, а держали строку». `[AI]` values, for the
 *  owner's feel on the Titan. */
private val ROW_TO_CARD = 24.dp
private val CARD_TO_ROW = 8.dp
/** The drag handle's height: the 5 dp bar and 8 dp above and below it (DragHandle). */
private val HANDLE_H = 21.dp
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
    val handlePx = with(density) { HANDLE_H.toPx() }
    // Collapsed into the row by default (the owner, 2026-10-16 ≈ 12:46: «по-умолчанию открывать со свернутым фото в строку»).
    var heroPx by remember { mutableFloatStateOf(minPx) }
    var fullscreen by remember { mutableStateOf(false) }
    // Where the preview starts on the screen, and the screen's height: the handle may travel to the bottom, and the 80 %
    // line of FULL_SCREEN_AT is measured on the whole screen.
    var previewTop by remember { mutableFloatStateOf(0f) }
    val screenH = LocalWindowInfo.current.containerSize.height.toFloat()
    val heroLimit = (screenH - previewTop - handlePx).coerceAtLeast(minPx)
    val collapse = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0) return Offset.Zero
                // An upward move of the settings first shrinks the preview, down to the row.
                val next = (heroPx + available.y).coerceAtLeast(minPx)
                val used = next - heroPx
                heroPx = next
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // A downward scroll of the settings at their top grows the preview back to HERO_MAX; further only the
                // handle pulls it (a fling of the list must not throw the photo across the screen).
                if (available.y <= 0 || heroPx >= maxPx) return Offset.Zero
                val next = (heroPx + available.y).coerceAtMost(maxPx)
                val used = next - heroPx
                heroPx = next
                return Offset(0f, used)
            }
        }
    }
    val heroDp = with(density) { heroPx.toDp() }
    // Row or card, with hysteresis: the switch points differ by direction, so the layout never flips under a resting finger.
    var asRow by remember { mutableStateOf(true) }
    if (asRow && heroDp >= HERO_MIN + ROW_TO_CARD) asRow = false
    if (!asRow && heroDp <= HERO_MIN + CARD_TO_ROW) asRow = true
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
        SetupOverlays(src.preview, fullscreen, { fullscreen = false }, info, { info = null }, vm::openManual)
        return
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().nestedScroll(collapse)) {
            SetupTopBar(vm)
            // Pinned preview: the photo card; at its floor it is the compact row. The handle and the photo (card or row)
            // pull it 1:1 with the finger and it stays where it is let go - no snap points (the owner, 2026-10-16 ≈ 12:46:
            // «Драг хендл чтобы не прилипал к конкретным местам, а превью фото разворачивалось на такой размер, на который
            // драг хендл пролистнули»). Let go with the handle below FULL_SCREEN_AT of the screen, the photo opens full
            // screen and the preview goes back to the size it had before that pull. [NOT-TESTED]
            var heroBefore by remember { mutableFloatStateOf(minPx) }
            val pull = rememberDraggableState { d -> heroPx = (heroPx + d).coerceIn(minPx, heroLimit) }
            val drag = Modifier.draggable(
                pull, Orientation.Vertical,
                onDragStarted = { heroBefore = heroPx },
                onDragStopped = {
                    val handleCentre = previewTop + heroPx + handlePx / 2
                    if (handleCentre > screenH * FULL_SCREEN_AT) { fullscreen = true; heroPx = heroBefore }
                },
            )
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp).onGloballyPositioned { previewTop = it.positionInRoot().y }) {
                if (asRow) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = heroDp).then(drag).clip(MaterialTheme.shapes.medium).background(c.surfaceContainer).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // The row grows to show the whole size change - three lines on a narrow phone - instead of clipping it
                        // (the owner, 2026-10-16 ≈ 21:4x: «оно обязано было показать результирующее разрешение, растянуться, чтобы
                        // оно поместилось»). [NOT-TESTED]
                        Image(src.preview.asImageBitmap(), src.name, Modifier.size(64.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            // The whole name, in the narrower text font and smaller, wrapping onto more lines - the wide heading font cut it to
                            // «IMG_20261…» (the owner, 2026-10-16 ≈ 21:5x: «название можно шрифтом поменьше, поуже, и переносить на несколько
                            // строк»). [NOT-TESTED]
                            Text(ltr(src.name), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = NAME_LINES, overflow = TextOverflow.MiddleEllipsis)
                            SizeChange(sizeText(src.width, src.height),
                                vm.limited?.let { (lw, lh) -> sizeText(lw, lh) } ?: sizeText(src.width * vm.scale, src.height * vm.scale),
                                MaterialTheme.typography.bodySmall, c.onSurfaceVariant)
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
    SetupOverlays(src.preview, fullscreen, { fullscreen = false }, info, { info = null }, vm::openManual)
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
            src.preview.asImageBitmap(), null,
            Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y },
            contentScale = ContentScale.Crop,
        )
        Pill("${ltr(sizeText(src.width, src.height))} · ${stringResource(R.string.megapixels, mpText(src.megapixels))}",
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
    vm.recentModels.forEach { m -> ModelCard(m, m == vm.model, m == vm.model && vm.modelLoading, durationText(vm.estimateMs(m))) { vm.chooseModel(m) } }
    // A model picked in «Все модели» takes the top card (vm.pickFromCatalogue), so the row below stays a plain button -
    // no third card above it (the owner, chat 2026-10-16, before 10:48 +03:00: «Кнопка Все модели не принимает в себя вид
    // модели, остается обычной кнопкой ведущей на экран списка моделей»).
    AllModelsRow(vm.catalogue.models.size, vm.onDevice.size, vm::openModels)
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

/** «Все модели · Моделей: 14 · на устройстве: 2 · Выбрать ›» under the cards (mockup A-setup, .row-link). */
@Composable
private fun AllModelsRow(total: Int, onDevice: Int, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(c.surfaceContainer).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.models_all), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.models_count, total, onDevice), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        Text(stringResource(R.string.models_choose), style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = c.primary)
        Spacer(Modifier.width(4.dp))
        Chevron(c.primary)
    }
}

/** Lines the photo's file name may take in the collapsed photo row before it is shortened in the middle. */
private const val NAME_LINES = 3

/** A model card (.mrow): name, what it is for with the estimate, the licence badge; pink border + tick when chosen. */
@Composable
private fun ModelCard(m: Model, on: Boolean, loading: Boolean, estimate: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (on) Brush.horizontalGradient(listOf(c.primaryContainer, c.surfaceContainer)) else Brush.linearGradient(listOf(c.surfaceContainer, c.surfaceContainer)))
            .border(2.dp, if (on) c.primary else c.surfaceContainer, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        // A gap between the text and the tick: the German description «… ≈ 1 Minute 5 Sekunden» ran into it (RC smoke on
        // the Titan, 2026-10-16). [NOT-TESTED]
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                ModelInfoButton(m)
            }
            if (m.starred) ChoiceLabel()
            Text(glueDots(stringResource(R.string.model_desc_with_estimate, stringResource(purposeRes(m.purpose)), estimate)), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            LicenceBadge(m)
        }
        // While the chosen model loads (preload on this screen) a small pink ring stands where the tick will be.
        if (on && loading) PixelSpinner(Modifier.size(22.dp))
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
    // Over the output limit the slider ends at the first step that does not fit, and that step reads as the factor that
    // does («3,2×» - plans/08 step 9, the owner 2026-10-16 ≈ 16:10: «да, правильно»). [TESTED: 2026-10-16 · Titan 50 MP: 1× · 2× · 3,2× - testcases/reports/2026-10-16_fractional_scale_titan.md]
    val last = vm.scaleCap
    val limited = vm.limited
    Column(Modifier.padding(horizontal = 6.dp)) {
        // The native Material 3 slider, styled like the quality slider of the save dialog (the owner, chat 2026-10-14
        // ≈ 22:41 +03:00: «как ползунок качества сделал - по дизайну современного андроид. Так и сделай в таком стиле выбор
        // масштаба» · «нативным элементом ползунка»): five positions, the chosen one lit in the marks below.
        // The slider and its marks run left to right in every language: in Arabic the slider mirrored (its fill ran toward
        // ×16 on the right of the 4× thumb) while the marks kept ×1 on the left (RC2 check on the Titan, 2026-10-16) -
        // numbers and the size line stay left-to-right as well. [NOT-TESTED]
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Slider(
            value = minOf(vm.scaleIndex, last).toFloat(), onValueChange = { vm.scaleIndex = it.roundToInt().coerceIn(0, last) },
            valueRange = 0f..maxOf(last, 1).toFloat(), steps = maxOf(last - 1, 0),
            colors = SliderDefaults.colors(thumbColor = c.onBackground, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceContainerHigh),
        )
        // Step marks at 0, 25, 50, 75, 100 % - the first left-aligned, the last right-aligned, the rest centred.
        Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            val locale = LocalConfiguration.current.locales[0]
            UpscaleViewModel.SCALES.take(last + 1).forEachIndexed { i, s ->
                // Plain «2×», no «по умолч.» (the owner, chat 2026-10-14 ≈ 20:49 +03:00: «убрать строчку По умолч»).
                val label = if (i == last && limited != null)
                    String.format(locale, "%.1f×", UpscaleViewModel.limitedFactor(src.width, limited.first)) else "$s×"
                Text(
                    label,
                    Modifier.layout { measurable, constraints ->
                        val pl = measurable.measure(constraints.copy(minWidth = 0))
                        val at = constraints.maxWidth * i / last
                        val left = when (i) { 0 -> 0; last -> at - pl.width; else -> at - pl.width / 2 }
                        layout(constraints.maxWidth, pl.height) { pl.place(left, 0) }
                    },
                    style = if (i == minOf(vm.scaleIndex, last)) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    color = if (i == minOf(vm.scaleIndex, last)) c.primary else c.onSurfaceVariant,
                )
            }
        }
        }
        val w = limited?.first ?: (src.width * vm.scale)
        val h = limited?.second ?: (src.height * vm.scale)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            SizeChange(sizeText(src.width, src.height), sizeText(w, h), MaterialTheme.typography.bodyMedium, c.onBackground, Modifier.weight(1f))
            Text(stringResource(R.string.megapixels, mpText(w.toFloat() * h / 1_000_000f)), style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
        }
        // 8× and 16× run two passes (plans/04 step 1): the mockup's amber «2 прохода · дольше · большой файл»; a result over
        // the output limit is refused with the largest scale that fits this photo.
        val notice = when {
            limited != null -> stringResource(R.string.scale_limited, UpscaleViewModel.MAX_OUTPUT_MP.toInt().toString(), vm.scale,
                String.format(LocalConfiguration.current.locales[0], "%.1f", UpscaleViewModel.limitedFactor(src.width, limited.first)),
                ltr(sizeText(limited.first, limited.second)))
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
