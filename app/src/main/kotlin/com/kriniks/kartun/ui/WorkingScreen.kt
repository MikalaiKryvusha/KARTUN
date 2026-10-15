package com.kriniks.kartun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

/** Width of the side panel with the ring in landscape. */
private val PANEL_WIDTH = 420.dp

/**
 * Processing (mockup 'processing'): the photo with the engine's tile grid over it - finished tiles clear, the current one
 * pink - and the sheet with the PERCENT ring, «Тайл N из M · осталось ≈ T», the backend pill and «Отменить» (cancel lands
 * between tiles). Percent ring and estimate: [OWNER] interview #003, Q1 = D. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
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
                Image(src.bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                val cols = ceil(src.width / tileOnPhoto).toInt()
                val rows = ceil(src.height / tileOnPhoto).toInt()
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
                            i == doneInPass -> { drawRect(now, o, s); drawRect(pink, o, s, style = Stroke(1.dp.toPx())) }
                            else -> drawRect(shade, o, s)
                        }
                        drawRect(line, o, s, style = Stroke(0.5.dp.toPx()))
                    }
                }
            }
        }
    }
    val panel: @Composable (Modifier) -> Unit = { m ->
        Column(
            m.background(c.surfaceContainerLow)
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Rounded ends of the arc (the owner, chat 2026-10-15 ≈ 11:35 +03:00: «если можно у кольца прогресс бара концы
                // сделать закругленными»).
                CircularProgressIndicator(
                    progress = { fraction }, modifier = Modifier.size(150.dp), color = c.primary,
                    strokeWidth = 11.dp, trackColor = c.surfaceContainerHigh, strokeCap = StrokeCap.Round, gapSize = 0.dp,
                )
                // Tabular figures: every digit takes the same width, so the number does not jitter as it counts (the owner,
                // plan 03 step 20: «в кольце прогресса цифры процентов пляшут, вероятно из-за разной ширины цифр»).
                Text(stringResource(R.string.percent, (fraction * 100).toInt()),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight(800), fontFeatureSettings = "tnum"))
            }
            Text(stringResource(R.string.working_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
            Text(
                if (p.total > 0 && mid != null) stringResource(R.string.working_pass, if (inPass2) 2 else 1, p.done, p.total, durationText(leftMs))
                else if (p.total > 0) stringResource(R.string.working_tiles, p.done, p.total, durationText(leftMs))
                else stringResource(R.string.working_starting, durationText(p.estimateMs)),
                style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Row(
                Modifier.clip(CircleShape).background(c.surfaceContainer).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_cpu), null, Modifier.size(16.dp), tint = c.primary)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (p.gpu) R.string.backend_gpu else R.string.backend_cpu), style = MaterialTheme.typography.labelMedium)
            }
            SecondaryButton(stringResource(R.string.cancel), null, onClick = vm::cancel)
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
