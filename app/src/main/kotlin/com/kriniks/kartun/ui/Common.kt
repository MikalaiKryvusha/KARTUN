package com.kriniks.kartun.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kriniks.kartun.R
import com.kriniks.kartun.ui.theme.StarGold
import java.util.Locale

/*
 * Small building blocks shared by the screens, each one a component of the accepted mockups
 * (design/kartun-mockups.html): round icon buttons (.icon-btn), pills (.pill), the pink primary button (.btn.primary),
 * section labels with an (i) button (.label + .info), the (i) bottom sheet. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */

/** Size of the chevron that ends a row or a link: the 16 sp «›» glyph was invisible even on the tablet (the owner,
 *  2026-10-16 ≈ 17:46: «шевроны на кнопочках > нужно больше сделать. Сейчас малые даже на планшете, не видно их»). */
private val CHEVRON = 20.dp

/** The one chevron of every row and link (settings rows, the guide link, «Выбрать»): the back button's thin stroke arrow
 *  turned to point forward, not a font glyph (the owner ≈ 17:48: «другой шрифт поставить, чтобы они были локаничными,
 *  тонкими, аля кнопка Назад <»). ic_back is auto-mirrored, so in Arabic the chevron points left (the owner ≈ 17:55:
 *  «арабский перевод остаётся, с зеркалирвоанием интерфейса делаем»). Decorative: the row's text names it for TalkBack.
 *  [NOT-TESTED] */
@Composable
fun Chevron(color: androidx.compose.ui.graphics.Color) {
    Icon(painterResource(R.drawable.ic_back), null, Modifier.size(CHEVRON).rotate(180f), tint = color)
}

/** 44 dp round dark button with a stroke icon (.icon-btn). */
@Composable
fun RoundIconButton(icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, translucent: Boolean = false, enabled: Boolean = true) {
    // Translucent = the theme's grey card at 85 %: visible on the black bars and on a photo alike (the owner, chat 2026-10-14
    // ≈ 20:54 +03:00: «почему у кнопки назад нет видимой подложки, хоть чуть-чуть серой»; it was the near-black background at 55 %).
    val bg = if (translucent) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier.size(44.dp).clip(CircleShape).background(bg).alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // The button's name for TalkBack and for the UI agent (HOUSE_RULES R7 - «где локатора нет, добавь его»): the gear
        // on the home screen had none, so the release walk of 2026-10-16 could not find «Настройки». Taken from the icon,
        // so no call site can forget it. [NOT-TESTED]
        val name = when (icon) {
            R.drawable.ic_back -> stringResource(R.string.action_back)
            R.drawable.ic_gear -> stringResource(R.string.settings_title)
            else -> null
        }
        Icon(painterResource(icon), name, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground)
    }
}

/** The pink 60 dp pill button with the main action of a screen (.btn.primary). */
@Composable
fun PrimaryButton(text: String, icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Int = 60) {
    val pink = MaterialTheme.colorScheme.primary
    Row(
        modifier.fillMaxWidth().height(height.dp)
            .shadow(if (enabled) 18.dp else 0.dp, CircleShape, ambientColor = pink, spotColor = pink)
            .clip(CircleShape).background(if (enabled) pink else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        if (icon != null) { Icon(painterResource(icon), null, Modifier.size(22.dp), tint = fg); Spacer(Modifier.width(10.dp)) }
        // One line that shrinks a little instead of wrapping: «Улучшить · ≈ 1 minute 25 / seconds» broke in two on 360 dp
        // phones with the icon pressed to the edge (RC smoke on the S21 FE, 2026-10-16); the words stay whole (the owner,
        // 2026-10-14 ≈ 18:58: «минут секунд в кнопке улучшения писать целым слвом»). [NOT-TESTED]
        val style = MaterialTheme.typography.labelLarge.copy(color = fg)
        androidx.compose.foundation.text.BasicText(
            text, style = style, maxLines = 1, softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = BUTTON_MIN_SP.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
        )
    }
}

/** The smallest size a primary button's label shrinks to before it is cut. */
private const val BUTTON_MIN_SP = 11f

/** Width of a small «Отменить» that must not stretch its card (the render card, the processing panel B1): 104 dp after the
 *  owner's «Кнопку отменить меньше, ниже» · «чтобы высвободить место для заголовка» (chat 2026-10-15 ≈ 12:35 +03:00). */
val COMPACT_BUTTON = 104.dp
/** «Остановить задание по API» on the Working screen: small and to the left (mockup C, design/processing-waiting.html). */
val STOP_API_BUTTON = 260.dp

/** Outlined (.btn.ghost) or filled-dark (.btn.soft) secondary pill button; it fills its width - pass a width to keep it small. */
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
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = when { height <= 40 -> 14.sp; height < 60 -> 16.sp; else -> 18.sp }), color = fg)
    }
}

/** A small rounded chip (.pill); [on] paints it pink, [outlined] gives the transparent bordered variant. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, on: Boolean = false, outlined: Boolean = false, translucent: Boolean = false,
         heavyDigits: Boolean = false, onClick: (() -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    var m = modifier.clip(CircleShape)
    m = when {
        on -> m.background(c.primary)
        outlined -> m.border(1.5.dp, c.outline, CircleShape)
        translucent -> m.background(c.surfaceContainerHigh.copy(alpha = 0.7f)) // grey see-through, like the translucent round button
        else -> m.background(c.surfaceContainer)
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Text(if (heavyDigits) boldDigits(text) else androidx.compose.ui.text.AnnotatedString(text), m.padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium, color = if (on) c.onPrimary else c.onBackground)
}

/** Pink upper-case section label with the (i) button on the right (.label + .info). */
@Composable
fun SectionLabel(text: String, onInfo: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(LocalConfiguration.current.locales[0]), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        InfoButton(text, onInfo)
    }
}

/** The (i) button every setting carries (AGENT_GUIDE.md -> Code style; the owner's rule from KrinikCam). */
@Composable
fun InfoButton(about: String, onClick: () -> Unit) {
    // TalkBack reads «Подробнее: Масштаб», not the letter «i» (the owner, 2026-10-16 ≈ 17:43: «TalkBack должен работать
    // корректно. Все им должно быть описано»). [NOT-TESTED]
    val name = stringResource(R.string.info_about, about)
    Box(
        Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = name; role = Role.Button },
        contentAlignment = Alignment.Center,
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
fun InfoDialog(title: String, text: String, list: String, onGuide: (() -> Unit)? = null, action: Pair<String, () -> Unit>? = null, onClose: () -> Unit) {
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
                    Chevron(MaterialTheme.colorScheme.primary)
                }
            }
            if (action != null) PrimaryButton(action.first, null, onClick = action.second, height = 54)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Pill(stringResource(R.string.info_close), on = action == null, outlined = action != null, onClick = onClose)
            }
        }
    }
}

/** A dismissible amber notice for user-facing errors (understandable words, never exception codes - GOAL.md §3). */
@Composable
fun Notice(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, success: Boolean = false) {
    // Amber for warnings and errors; green for a success (the owner, chat 2026-10-14 ≈ 22:42 +03:00: «это позитивный успех -
    // зелёным цветом об успехе говорят, а не жёлтым»).
    val tone = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Text(
        text,
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(tone.copy(alpha = 0.14f))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelMedium, color = tone,
    )
}

/**
 * «960 × 1280 → 3840 × 5120» on one line when it fits; when it does not, three centred lines - the source size, a pink
 * arrow down, the result size - instead of a break inside a size (the owner, 2026-10-16 ≈ 20:4x: «если разрешение
 * приходится перенести на вторую строку - то переноси на третью, а на товрой пусть будет розовая стрелочка по центру
 * вниз»). The one place every screen shows a size change. [NOT-TESTED]
 */
@Composable
fun SizeChange(from: String, to: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val line = boldDigits(stringResource(R.string.size_to, ltr(from), ltr(to)))
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val oneLine = measurer.measure(line, style, maxLines = 1, softWrap = false).size.width
        if (oneLine <= constraints.maxWidth) {
            Text(line, style = style, color = color, maxLines = 1, softWrap = false)
        } else {
            Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(boldDigits(ltr(from)), style = style, color = color, maxLines = 1, softWrap = false)
                Text("↓", style = style.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                Text(boldDigits(ltr(to)), style = style, color = color, maxLines = 1, softWrap = false)
            }
        }
    }
}

/**
 * A left-to-right run (a size, a file name, a version) kept whole inside right-to-left text: Unicode «left-to-right isolate»
 * (U+2066 … U+2069). In Arabic «3840 × 5120» showed as «5120 × 3840», «IMG_1.jpg» as «jpg.IMG_1», «16 (API 36)» as
 * «(API 36) 16» (RC smoke on the Titan, 2026-10-16); in left-to-right languages the marks change nothing. [NOT-TESTED]
 */
fun ltr(s: String): String = "$LTR_ISOLATE$s$POP_ISOLATE"

/** The isolate marks by code point, not as literals (lint's BidiSpoofing rightly flags raw bidi controls in source). */
private val LTR_ISOLATE = Char(0x2066)
private val POP_ISOLATE = Char(0x2069)

/** «★ ВЫБОР KARTUN» - a gold chip under a favourite model's name, shaped like the licence badges (Model.starred; the owner,
 *  2026-10-16 ≈ 21:4x: «золотая звезда и ниже золотом текст ВЫБОР KARTUN» · ≈ 21:5x: «тут звезду слева от текста ВЫБОР
 *  ставь» · «давай выбор картун превратим в золотой чипс, и звезду выше чуть-чуть поднимем»). [NOT-TESTED] */
@Composable
fun ChoiceLabel() = Row(
    Modifier.clip(CircleShape).background(StarGold.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    // The star's glyph sits low in its line box: lifted to the capitals' middle.
    Text("★", Modifier.offset(y = STAR_LIFT), style = MaterialTheme.typography.labelMedium, color = StarGold)
    Spacer(Modifier.width(5.dp))
    Text(stringResource(R.string.model_choice), style = MaterialTheme.typography.labelSmall, color = StarGold)
}

/** How far the star of [ChoiceLabel] is lifted to line up with the capitals. */
private val STAR_LIFT = (-1.5).dp

/** "3840 × 5120"; numbers from 10 000 get a thin space as the mockups show ("7 680 × 10 240"). Pure format - the screens wrap
 *  it in [ltr] where they show it (FormatTest guards the format). */
fun sizeText(w: Int, h: Int): String = "${group(w)} × ${group(h)}"

/**
 * A resolution line with heavy digits, thin «×» and spaces (the owner, chat 2026-10-15 ≈ 11:58 +03:00: «циферки в
 * разрешениях нужно жирнее… x оставлять тонкими, а цифры толще»), and the arrow pink and heavy (≈ 12:33: «давай
 * стрелочку… увеличения разрешения в розовый наш покрасим, и сделаем толще»; it was thin by the 11:58 word). A space
 * between two digits is the thousands separator of [group] and stays with its number. [NOT-TESTED]
 */
@Composable
fun boldDigits(s: String): androidx.compose.ui.text.AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    return androidx.compose.ui.text.buildAnnotatedString {
        append(s)
        val heavy = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)
        Regex("\\d+(?:[ \\u00a0\\u202f]\\d{3})*").findAll(s).forEach { addStyle(heavy, it.range.first, it.range.last + 1) }
        val arrow = androidx.compose.ui.text.SpanStyle(color = accent, fontWeight = FontWeight.ExtraBold)
        Regex("[→←]").findAll(s).forEach { addStyle(arrow, it.range.first, it.range.last + 1) }
    }
}

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
