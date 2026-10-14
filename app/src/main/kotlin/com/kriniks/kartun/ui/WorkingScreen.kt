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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriniks.kartun.R
import com.kriniks.kartun.UpscaleViewModel
import kotlin.math.ceil
import kotlin.math.min

/** Tile edge the engine uses - the grid over the photo mirrors the engine's real tiles. */
private const val TILE_PX = 200

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

    // Column, not an overlay: the photo takes the space above the sheet, so no tile hides under it (seen on the Titan
    // 2026-10-14 18:41 - a fixed bottom padding was shorter than the sheet).
    Column(Modifier.fillMaxSize().background(c.background)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).statusBarsPadding().padding(16.dp)) {
            // Photo fitted into the free area; the grid is drawn over the fitted rectangle only.
            val k = min(maxWidth.value / src.width, maxHeight.value / src.height)
            val w = (src.width * k).dp
            val h = (src.height * k).dp
            Box(Modifier.align(Alignment.Center).size(w, h)) {
                Image(src.bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                val cols = ceil(src.width / TILE_PX.toFloat()).toInt()
                val rows = ceil(src.height / TILE_PX.toFloat()).toInt()
                val shade = c.background.copy(alpha = 0.62f)
                val now = c.primary.copy(alpha = 0.28f)
                val line = c.onBackground.copy(alpha = 0.08f)
                val pink = c.primary
                Canvas(Modifier.fillMaxSize()) {
                    val tw = size.width * TILE_PX / src.width
                    val th = size.height * TILE_PX / src.height
                    for (r in 0 until rows) for (col in 0 until cols) {
                        val i = r * cols + col
                        val o = Offset(col * tw, r * th)
                        val s = Size(min(tw, size.width - o.x), min(th, size.height - o.y))
                        when {
                            i < p.done -> Unit
                            i == p.done -> { drawRect(now, o, s); drawRect(pink, o, s, style = Stroke(1.dp.toPx())) }
                            else -> drawRect(shade, o, s)
                        }
                        drawRect(line, o, s, style = Stroke(0.5.dp.toPx()))
                    }
                }
            }
        }
        // Bottom sheet with the ring.
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(c.surfaceContainerLow)
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction }, modifier = Modifier.size(150.dp), color = c.primary,
                    strokeWidth = 11.dp, trackColor = c.surfaceContainerHigh, strokeCap = StrokeCap.Butt, gapSize = 0.dp,
                )
                Text(stringResource(R.string.percent, (fraction * 100).toInt()), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight(800)))
            }
            Text(stringResource(R.string.working_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
            Text(
                if (p.total > 0) stringResource(R.string.working_tiles, p.done, p.total, durationText(leftMs))
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
}
