package com.kriniks.kartun.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriniks.kartun.R

/**
 * Krinik's style for KARTUN - the ONE place colours, shapes and type live (AGENT_GUIDE.md -> Code style: no per-file
 * colour constants). Values are the tokens of the accepted mockups `design/kartun-mockups.html` (`:root` block):
 * near-black background, flat dark cards, one pink accent #FF1A8C. Dark theme only (plans/01 §4.6). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
private val Pink = Color(0xFFFF1A8C)          // --pink
private val Bg = Color(0xFF0D0D0D)            // --bg
private val Surface = Color(0xFF151516)       // --surface (sheets, panels)
private val Card = Color(0xFF1C1C1E)          // --card
private val Card2 = Color(0xFF262628)         // --card2 (tracks, inner chips)
private val Line = Color(0xFF2E2E31)          // --line
private val Text = Color(0xFFF5F5F7)          // --text
private val Sub = Color(0xFFA1A1A6)           // --sub
private val Mute = Color(0xFF6E6E73)          // --mute
private val PinkSoft = Color(0x24FF1A8C)      // --pink-soft, rgba(255,26,140,.14)
private val Green = Color(0xFF3DDC84)         // the "on device" dot and badge
private val Amber = Color(0xFFFFB020)         // warnings (8x/16x, licences)
private val Blue = Color(0xFF8FB8FF)          // the «free with credit» (CC BY) licence badge (mockup .badge.by)
/** The gold star of the models the owner rated best in the blind tests (2026-10-16 ≈ 21:4x: «поставим золотые звезды тем
 *  можелям, которые я больше всего оценил» · «это 4 модели»). [NOT-TESTED] */
val StarGold = Color(0xFFFFC83D)
/** The «not for commercial use» licence badge: violet text on its dark violet tint (the owner, 2026-10-16 ≈ 21:5x: «не для
 *  комерческого использования давай покрасим в темный фиолетовый»; it was amber). [NOT-TESTED] */
val NonCommercialViolet = Color(0xFFA673FF)  // ≈ 21:5x: «можно чуть-чуть тяжелее фиолетовый» (was B98CFF)

private val Colors = darkColorScheme(
    primary = Pink, onPrimary = Color.White, primaryContainer = PinkSoft, onPrimaryContainer = Pink,
    background = Bg, onBackground = Text,
    surface = Bg, onSurface = Text,
    surfaceContainerLowest = Bg, surfaceContainerLow = Surface, surfaceContainer = Card,
    surfaceContainerHigh = Card2, surfaceContainerHighest = Card2,
    onSurfaceVariant = Sub, outline = Line, outlineVariant = Mute,
    secondary = Blue, tertiary = Green, error = Amber, onError = Color.Black,
    scrim = Color(0x8C000000),
)

// Unbounded for headings and the logo, Manrope for text ([OWNER] interview #002, Q2 = A). Both are variable OFL fonts
// (res/font, licences in docs/licenses/); each weight is a variation of the one file.
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

private val Unbounded = FontFamily(variable(R.font.unbounded, 600), variable(R.font.unbounded, 700), variable(R.font.unbounded, 800))
private val Manrope = FontFamily(
    variable(R.font.manrope, 400), variable(R.font.manrope, 500), variable(R.font.manrope, 600),
    variable(R.font.manrope, 700), variable(R.font.manrope, 800),
)

private val Type = Typography(
    displaySmall = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(800), fontSize = 24.sp, letterSpacing = 0.5.sp),
    headlineSmall = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(700), fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(700), fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(700), fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(700), fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 18.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(600), fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 12.sp, letterSpacing = 1.4.sp),
)

// --r-card 24, --r-big 28, model cards 20; pills are fully round (CircleShape at the call site).
private val KShapes = Shapes(
    small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp),
)

// Text without an explicit colour takes the theme's text colour - the screens draw on plain Boxes, not on a Surface,
// so without this provider such text came out black (seen on the Titan 2026-10-14 18:37, home screen).
@Composable
fun KartunTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Colors, typography = Type, shapes = KShapes) {
        CompositionLocalProvider(LocalContentColor provides Colors.onBackground, content = content)
    }
