package com.kriniks.kartun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kriniks.kartun.R
import java.util.Locale

/*
 * Small building blocks shared by the screens, each one a component of the accepted mockups
 * (design/kartun-mockups.html): round icon buttons (.icon-btn), pills (.pill), the pink primary button (.btn.primary),
 * section labels with an (i) button (.label + .info), the (i) bottom sheet. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */

/** 44 dp round dark button with a stroke icon (.icon-btn). */
@Composable
fun RoundIconButton(icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, translucent: Boolean = false, enabled: Boolean = true) {
    val bg = if (translucent) MaterialTheme.colorScheme.background.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier.size(44.dp).clip(CircleShape).background(bg).alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground) }
}

/** The pink 60 dp pill button with the main action of a screen (.btn.primary). */
@Composable
fun PrimaryButton(text: String, icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Int = 60) {
    val pink = MaterialTheme.colorScheme.primary
    Row(
        modifier.fillMaxWidth().height(height.dp)
            .shadow(if (enabled) 18.dp else 0.dp, CircleShape, ambientColor = pink, spotColor = pink)
            .clip(CircleShape).background(if (enabled) pink else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        if (icon != null) { Icon(painterResource(icon), null, Modifier.size(22.dp), tint = fg); Spacer(Modifier.width(10.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Outlined (.btn.ghost) or filled-dark (.btn.soft) secondary pill button. */
@Composable
fun SecondaryButton(text: String, icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier, outlined: Boolean = true, enabled: Boolean = true, height: Int = 60) {
    val shape = CircleShape
    var m = modifier.fillMaxWidth().height(height.dp).clip(shape)
    m = if (outlined) m.border(1.5.dp, MaterialTheme.colorScheme.outline, shape) else m.background(MaterialTheme.colorScheme.surfaceContainer)
    Row(
        m.alpha(if (enabled) 1f else 0.45f).clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = MaterialTheme.colorScheme.onBackground
        if (icon != null) { Icon(painterResource(icon), null, Modifier.size(22.dp), tint = fg); Spacer(Modifier.width(10.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = if (height < 60) 16.sp else 18.sp), color = fg)
    }
}

/** A small rounded chip (.pill); [on] paints it pink, [outlined] gives the transparent bordered variant. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, on: Boolean = false, outlined: Boolean = false, translucent: Boolean = false, onClick: (() -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    var m = modifier.clip(CircleShape)
    m = when {
        on -> m.background(c.primary)
        outlined -> m.border(1.5.dp, c.outline, CircleShape)
        translucent -> m.background(c.background.copy(alpha = 0.72f))
        else -> m.background(c.surfaceContainer)
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Text(text, m.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = if (on) c.onPrimary else c.onBackground)
}

/** Pink upper-case section label with the (i) button on the right (.label + .info). */
@Composable
fun SectionLabel(text: String, onInfo: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(LocalConfiguration.current.locales[0]), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        InfoButton(onInfo)
    }
}

/** The (i) button every setting carries (AGENT_GUIDE.md -> Code style; the owner's rule from KrinikCam). */
@Composable
fun InfoButton(onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp).border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
            Text("i", color = MaterialTheme.colorScheme.outlineVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

/**
 * The (i) dialog: a title, the explanation and a list - texts from the user manual of the mockups. A centred dialog,
 * not a bottom sheet: the owner's word in chat 2026-10-14 ≈ 18:48 «Инфо открывать диалогами, а не ботом шитами».
 */
@Composable
fun InfoDialog(title: String, text: String, list: String, onGuide: (() -> Unit)? = null, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (list.isNotEmpty()) Text(list, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // «Открыть руководство полностью ›» (mockup 'info-scale' .row-link): the (i) is an excerpt of the guide.
            if (onGuide != null) {
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onGuide).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.info_open_guide), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Pill(stringResource(R.string.info_close), on = true, onClick = onClose)
            }
        }
    }
}

/** A dismissible amber notice for user-facing errors (understandable words, never exception codes - GOAL.md §3). */
@Composable
fun Notice(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error,
    )
}

/** "3840 × 5120"; numbers from 10 000 get a thin space as the mockups show ("7 680 × 10 240"). */
fun sizeText(w: Int, h: Int): String = "${group(w)} × ${group(h)}"

private fun group(n: Int): String =
    if (n < 10_000) n.toString() else String.format(Locale.ROOT, "%,d", n).replace(',', ' ')

/** Megapixels with one decimal in the user's locale ("1,2" in Russian). */
fun mpText(mp: Float): String = String.format(Locale.getDefault(), "%.1f", mp)

/**
 * A duration for people in whole words with the language's plural forms: «7 секунд», «1 минута 50 секунд», «2 минуты»
 * (the owner, 2026-10-14: «минут секунд в кнопке улучшения писать целым слвом»).
 */
@Composable
fun durationText(ms: Long): String {
    val (m, s) = durationParts(ms)
    val sec = pluralStringResource(R.plurals.duration_seconds, s, s)
    val min = pluralStringResource(R.plurals.duration_minutes, m, m)
    return when {
        m == 0 -> sec
        s == 0 -> min
        else -> "$min $sec"
    }
}

/** Whole minutes and seconds of [ms], rounded to the nearest second and never below 1 s (a run is never «0 секунд»). */
fun durationParts(ms: Long): Pair<Int, Int> {
    val total = ((ms + 500) / 1000).coerceAtLeast(1).toInt()
    return total / 60 to total % 60
}

/** Transparent-to-background fade above a pinned bottom button (.cta::before). */
fun fadeColors(bg: Color) = listOf(bg.copy(alpha = 0f), bg, bg)
