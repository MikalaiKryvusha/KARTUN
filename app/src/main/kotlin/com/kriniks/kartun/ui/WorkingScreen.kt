package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriniks.kartun.R
import com.kriniks.kartun.UpscaleViewModel
import kotlin.math.ceil
import kotlin.math.min

/** Below this panel width the four metric tiles stand two by two (each needs ~100 dp for «5 мин 40 с»). */
private val NARROW_PANEL = 440.dp

/** Width of the side panel with the ring in landscape. */
private val PANEL_WIDTH = 420.dp

/** How often the «Прошло» line ticks. */
private const val ELAPSED_TICK_MS = 1000L

/** The progress ring of the panel (B1: 84 dp, beside the title). */
private val RING = 84.dp

/** Pads a counter to the width of its total: digits and this space have the same width with tabular figures. */
private const val FIGURE_SPACE = '\u2007'

/**
 * One metric tile of the panel: a small upper-case label over a value in tabular figures. Both stay on ONE line and shrink
 * to the tile's width instead of being cut: «459 / 1 344» showed as «459 /» (maxLines = 1 kept only the first soft-wrapped
 * line) and «ОСТАЛО» lost its end in landscape - the owner, 2026-10-16 17:03 screenshots: «Смотри, чтобы эти плитки
 * уважительно работали с длинными текстами и номерами». [TESTED: 2026-10-16 · Titan, «21 / 2176» portrait and landscape - testcases/reports/2026-10-16_fractional_scale_titan.md]
 */
@Composable
internal fun Metric(label: String, value: String, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(c.surfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp)) {
        val labelStyle = MaterialTheme.typography.labelSmall
        val valueStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight(700), fontFeatureSettings = "tnum")
        FitText(label.uppercase(LocalConfiguration.current.locales[0]), labelStyle.copy(color = c.onSurfaceVariant), MIN_LABEL_SP)
        FitText(value, valueStyle.copy(color = c.onSurface), MIN_VALUE_SP)
    }
}

/** Smallest sizes a tile's label and value may shrink to before the text is cut; lowered and the title given a third line
 *  after the RC smoke on the Redmi (1080 px wide, English): «enhancing your ph…», «≈ 3 min …» were cut (2026-10-16). [NOT-TESTED] */
private const val TITLE_LINES = 3
private const val MIN_LABEL_SP = 8f
private const val MIN_VALUE_SP = 8f
private const val MIN_TITLE_SP = 10f

/** One line that shrinks from its style's size down to [minSp] to fit the width; never wraps, never cuts mid-word. */
@Composable
private fun FitText(text: String, style: androidx.compose.ui.text.TextStyle, minSp: Float, maxLines: Int = 1) {
    androidx.compose.foundation.text.BasicText(
        text, style = style, maxLines = maxLines, softWrap = maxLines > 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = minSp.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}

/** «1 мин 12 с» / «38 с» / «3 мин»: the short form that fits a metric tile (the long one is «1 минута 12 секунд»). */
@Composable
private fun shortDuration(ms: Long): String {
    val (m, s) = durationParts(ms)
    return when {
        m == 0 -> stringResource(R.string.duration_short_s, s)
        s == 0 -> stringResource(R.string.duration_short_m, m)
        else -> stringResource(R.string.duration_short_ms, m, s)
    }
}

/**
 * Processing (mockup 'processing'): the photo with the engine's tile grid over it - finished tiles clear, the current one
 * pink - and the sheet with the PERCENT ring, «Тайл N из M», «Прошло T», «Осталось ≈ T», the backend pill and «Отменить»
 * (cancel lands between tiles). Percent ring and estimate: [OWNER] interview #003, Q1 = D. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
@Composable
fun WorkingScreen(vm: UpscaleViewModel) {
    val src = vm.source ?: return
    val p by vm.progress.collectAsState()
    val c = MaterialTheme.colorScheme
    BackHandler { vm.cancel() }
    val fraction = if (p.total > 0) p.done.toFloat() / p.total else 0f
    // Remaining time: from the real pace once tiles report, from the estimate before that.
    val leftMs = if (p.done > 0) p.elapsedMs * (p.total - p.done) / p.done else p.estimateMs
    // 8× and 16× run a second pass over the 2× / 4× image (plans/04 step 1): its tiles are smaller on the photo, and the
    // grid starts over for it. pass1 = the photo's own tiles; anything above them belongs to pass 2.
    val mid = UpscaleViewModel.secondPassInput(vm.scale)
    val pass1 = UpscaleViewModel.tiles(src.width, src.height)
    val inPass2 = mid != null && p.done >= pass1
    val tileOnPhoto = if (inPass2) UpscaleViewModel.TILE_PX.toFloat() / mid!! else UpscaleViewModel.TILE_PX.toFloat()
    val doneInPass = if (inPass2) p.done - pass1 else p.done
    // Time already spent, ticking every second between tiles too (the owner, chat 2026-10-15 ≈ 12:18 +03:00: «Осталось -
    // ок. Нужно еще считать время, сколько уже длиться процесс»). The start is taken back from the engine's elapsed time,
    // so a recreated screen keeps counting from the run's real start. [NOT-TESTED]
    // Re-anchored when the engine takes the run: the wait behind an API job is not «Прошло».
    val startedAt = remember(p.started) { android.os.SystemClock.elapsedRealtime() - p.elapsedMs }
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { now = android.os.SystemClock.elapsedRealtime(); kotlinx.coroutines.delay(ELAPSED_TICK_MS) } }
    val spentMs = now - startedAt
    // Variant C of design/processing-waiting.html (the owner, 2026-10-16: «макет C принят, но не нужно писать про ИИ агента,а
    // нужно писать, что KARTUN занят заданием по API»): while an API job holds the engine and this run has not started, the
    // panel says «Ждёт очереди», counts the wait as «Ждёт» and offers «Остановить задание по API». [NOT-TESTED]
    val apiBusy by vm.apiBusy.collectAsState()
    val waiting = !p.started && apiBusy
    val waitFrom = remember { android.os.SystemClock.elapsedRealtime() }

    // Portrait: a column - the photo takes the space above the sheet, so no tile hides under it (seen on the Titan
    // 2026-10-14 18:41 - a fixed bottom padding was shorter than the sheet). Landscape (the owner: «лендскейп нужно
    // поддерживать тоже»): a row - the photo on the left, the panel on the right, centred in height. [NOT-TESTED: landscape - built only; the device run waits for the owner («потом потестируем»)]
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val photo: @Composable (Modifier) -> Unit = { m ->
        BoxWithConstraints(m.statusBarsPadding().padding(16.dp)) {
            // Photo fitted into the free area; the grid is drawn over the fitted rectangle only.
            val k = min(maxWidth.value / src.width, maxHeight.value / src.height)
            val w = (src.width * k).dp
            val h = (src.height * k).dp
            Box(Modifier.align(Alignment.Center).size(w, h)) {
                Image(src.preview.asImageBitmap(), src.name, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                val cols = ceil(src.width / tileOnPhoto).toInt()
                val rows = ceil(src.height / tileOnPhoto).toInt()
                // The tile being computed: a pink fill, no living animation on it (the owner, chat 2026-10-16, before 10:48
                // +03:00: «Убираем нашу живую анимацию из тайлов по время улучшения фото» - undoes «B · На тайле» of
                // 2026-10-15 19:52). [NOT-TESTED]
                val shade = c.background.copy(alpha = 0.62f)
                val now = c.primary.copy(alpha = 0.28f)
                val line = c.onBackground.copy(alpha = 0.08f)
                val pink = c.primary
                Canvas(Modifier.fillMaxSize()) {
                    val tw = size.width * tileOnPhoto / src.width
                    val th = size.height * tileOnPhoto / src.height
                    for (r in 0 until rows) for (col in 0 until cols) {
                        val i = r * cols + col
                        val o = Offset(col * tw, r * th)
                        val s = Size(min(tw, size.width - o.x), min(th, size.height - o.y))
                        when {
                            i < doneInPass -> Unit
                            // No tile is «now» while the run waits behind an API job - nothing is being computed for it.
                            i == doneInPass && !waiting -> { drawRect(now, o, s); drawRect(pink, o, s, style = Stroke(1.dp.toPx())) }
                            else -> drawRect(shade, o, s)
                        }
                        drawRect(line, o, s, style = Stroke(0.5.dp.toPx()))
                    }
                }
            }
        }
    }
    // The panel, variant B1 of design/processing-variants.html (the owner, chat 2026-10-15 ≈ 12:30 +03:00: «Давай Б1
    // попробуем, только кнопку Отменить вверх поднять над тайтлами метрик»; before that ≈ 12:17: the old panel took too
    // much height): the ring, the title with the backend and «Отменить» in one row, the four metrics as tiles under it.
    // [NOT-TESTED]
    val panel: @Composable (Modifier) -> Unit = { m ->
        Column(
            m.background(c.surfaceContainerLow)
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    // Rounded ends of the arc (the owner, 11:35: «если можно у кольца прогресс бара концы сделать закругленными»).
                    CircularProgressIndicator(
                        progress = { fraction }, modifier = Modifier.size(RING), color = c.primary,
                        strokeWidth = 8.dp, trackColor = c.surfaceContainerHigh, strokeCap = StrokeCap.Round, gapSize = 0.dp,
                    )
                    // Tabular figures: the number does not jitter as it counts (plan 03 step 20). Waiting: an hourglass.
                    Text(if (waiting) "⏳" else stringResource(R.string.percent, (fraction * 100).toInt()),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight(800), fontFeatureSettings = "tnum"))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 15 sp: at 17 the wide heading font cut the title even beside the small «Отменить» (Titan 12:37). Two
                    // lines and a shrink to 11 sp: landscape cut it to «Нейросеть улу…» (the owner's screenshot 2026-10-16 17:03).
                    FitText(stringResource(if (waiting) R.string.waiting_title else R.string.working_title),
                        MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, color = c.onSurface), MIN_TITLE_SP, maxLines = TITLE_LINES)
                    if (waiting) Text(stringResource(R.string.waiting_text), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    // «Отменить» on the chip's line, under the title - beside the title it squeezed it to «The network is enhancing
                    // your p…» (the owner, 2026-10-16 ≈ 21:0x: «я просил кнопку Cancel ниже, чтобы она не мешала заголовку» ·
                    // «вниз её, где чимп GPU Vulcan»). [NOT-TESTED]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!waiting) Row(
                            Modifier.clip(CircleShape).background(c.surfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_cpu), null, Modifier.size(14.dp), tint = c.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (p.gpu) R.string.backend_gpu else R.string.backend_cpu), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                        }
                        Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
                        SecondaryButton(stringResource(R.string.cancel), null, onClick = vm::cancel, height = 36, modifier = Modifier.width(COMPACT_BUTTON))
                    }
                }
            }
            // The counters stand still (the owner, 11:57: «чтобы они не плясали при изменении цифр в них»): tabular figures
            // and the done count padded with figure spaces to the total's width. The pass shows on one-pass scales too
            // («1 / 1» - the owner, 12:17: «и надпись номера прогона исчезла =(»). Elapsed ticks every second (12:18).
            val total = "%d".format(p.total)
            val metrics = if (waiting) listOf(
                stringResource(R.string.metric_pass) to "—",
                stringResource(R.string.metric_tile) to "—",
                stringResource(R.string.metric_waiting) to shortDuration(now - waitFrom),
                stringResource(R.string.metric_work) to "≈ " + shortDuration(p.estimateMs),
            ) else listOf(
                stringResource(R.string.metric_pass) to "${if (inPass2) 2 else 1} / ${if (mid != null) 2 else 1}",
                stringResource(R.string.metric_tile) to
                    (if (p.total > 0) "${"%d".format(p.done).padStart(total.length, FIGURE_SPACE)} / $total" else "—"),
                stringResource(R.string.metric_elapsed) to shortDuration(spentMs),
                stringResource(R.string.metric_left) to "≈ " + shortDuration(if (p.total > 0) leftMs else p.estimateMs),
            )
            // Portrait: one row of four. Landscape: the panel is PANEL_WIDTH wide, four tiles in a row left ~65 dp of text
            // each («ОСТАЛО», «≈ 4» on the owner's screenshot 2026-10-16 17:03) - two rows of two. [TESTED: 2026-10-16 · Titan landscape - testcases/reports/2026-10-16_fractional_scale_titan.md]
            // Phones in portrait are narrow too: four tiles on ~360 dp cut «5 min 4…», «≈ 1 min …» (RC smoke on the A52, S21 FE and
            // Redmi, 2026-10-16; the owner ≈ 21:0x: «на а52 ужасно выблядит экран с процессом») - two rows of two below NARROW_PANEL. [NOT-TESTED]
            BoxWithConstraints { val narrow = maxWidth < NARROW_PANEL; Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.chunked(if (landscape || narrow) 2 else 4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, value) -> Metric(label, value, Modifier.weight(1f)) }
                    }
                }
            } }
            // Small and to the left, as in the accepted mockup C - the main action stays «Отменить».
            if (waiting) SecondaryButton(stringResource(R.string.stop_api_job), null, onClick = vm::stopApiJob, height = 34,
                modifier = Modifier.width(STOP_API_BUTTON))
        }
    }
    if (landscape) {
        Row(Modifier.fillMaxSize().background(c.background)) {
            photo(Modifier.weight(1f).fillMaxHeight())
            panel(Modifier.width(PANEL_WIDTH).fillMaxHeight().clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)))
        }
    } else {
        Column(Modifier.fillMaxSize().background(c.background)) {
            photo(Modifier.fillMaxWidth().weight(1f))
            panel(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)))
        }
    }
}
