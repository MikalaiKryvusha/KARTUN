package com.kriniks.kartun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/*
 * The living pixel spinner - design/spinner-live.html carried over one to one (the owner, chat 2026-10-15 ≈ 12:37–13:00 +03:00:
 * «Как из пикселей рассыпанных что-то собирается?» · «давай 1, 3, 4, и чтобы они в разное время друг друга сменяли» · «чтобы
 * квадратики жили в анимации» · «медленее на 50%» · «лишние… сливаются. объединяются, или делятся» · «квадрат только
 * проявляется» · «пусть пиксели чуть-чуть словно разлетаются от взрыва» · verdict «разные анимации фигур красиво»).
 *
 * 36 squares gather into a heart, a pixel ring or a square of nine tiles; every 4-7 s they regroup into another figure.
 * A regroup opens with a small explosion, then each square flies by the regroup's mood (burst, swirl, rain, snake) and its
 * own character (straight, arc, shy, spinner, overshooter). A figure with fewer places merges the spare squares into the
 * nearest host; a figure that needs more splits them off again. In flight a square blends from the look it left with into
 * the new figure's look. The heart builds from the bottom and then plays colours at random; the tiles square only builds
 * and regroups once fully built; the ring runs a wave. All time is 1.5x slower than the first take. [NOT-TESTED]
 */

private const val N = 36
private const val SLOW = 1.5f
private const val BURST = 380f
/** The share of regroups that open with the small explosion. */
private const val EXPLODE_SHARE = .33f
private const val TWO_PI = (2 * PI).toFloat()

private fun clamp01(v: Float) = v.coerceIn(0f, 1f)
private fun easeInOut(t: Float) = if (t < .5f) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
private fun easeOut(t: Float) = 1 - (1 - t).pow(3)
private fun backOut(t: Float): Float { val s = 1.4f; return 1 + (s + 1) * (t - 1).pow(3) + s * (t - 1).pow(2) }

private class Pt(val x: Float, val y: Float)
private class Look(val k: Float, val c: Color, val dx: Float = 0f, val dy: Float = 0f)

private val HEART = listOf("0110110", "1111111", "1111111", "0111110", "0011100", "0001000")
private val HEART_PTS = HEART.flatMapIndexed { y, row ->
    row.mapIndexedNotNull { x, ch -> if (ch == '1') Pt((x + .5f) / 7 * .8f + .1f, (y + .5f) / 6 * .7f + .16f) else null }
}
private const val SQ0 = .13f
private const val SEAM = .025f
private const val TILE = (1 - 2 * SQ0 - 2 * SEAM) / 3
private const val TILE_SUB = TILE / 2

private enum class Fig(val size: Float, val count: Int, val stagger: Float) {
    Heart(.1f, HEART_PTS.size, 1000f), Ring(.085f, 18, 0f), Tiles(TILE_SUB, 36, 0f);

    fun target(i: Int): Pt = when (this) {
        Heart -> HEART_PTS[i]
        Ring -> { val a = i / 18f * TWO_PI - PI.toFloat() / 2; Pt(.5f + cos(a) * .36f, .5f + sin(a) * .36f) }
        Tiles -> {
            val j = i % 36; val tile = j / 4; val sub = j % 4; val tx = tile % 3; val ty = tile / 3
            Pt(SQ0 + tx * (TILE + SEAM) + (sub % 2 + .5f) * TILE_SUB, SQ0 + ty * (TILE + SEAM) + (sub / 2 + .5f) * TILE_SUB)
        }
    }
}

private enum class Mood { Burst, Swirl, Rain, Snake }
private enum class Who { Straight, Arc, Shy, Spinner, Overshooter }

private class Sq(rnd: Random) {
    var from = Pt(rnd.nextFloat(), rnd.nextFloat()); var kick: Pt? = null; var ctrl = Pt(.5f, .5f); var to = Pt(.5f, .5f)
    var start = 0f; var delay = 0f; var dur = 900f; var who = Who.Straight; var spin = 0f
    var host = -1; var merged = false; var splitAt = -1f; var gulpAt = -1f
    var look = Look(.8f, Color.Unspecified)
}

/** All the spinner's state; [update] moves it on, [draw] only reads it. */
private class PixelSpinnerState(private val pink: Color, private val grey: Color) {
    private val deep = lerp(pink, Color.Black, .53f)
    private val light = lerp(pink, Color.White, .45f)
    private val rnd = Random(System.nanoTime())
    val sq = Array(N) { Sq(rnd).also { it.look = Look(.8f, deep) } }
    var fig = Fig.Heart; private set
    private var mood = Mood.Burst
    private var switchAt = 0f
    private var formedAt = 0f

    private fun tilesLevel(t: Float) = ((t - formedAt - 700) / 260).coerceIn(0f, 10f)

    private fun life(f: Fig, i: Int, t: Float): Look = when (f) {
        Fig.Heart -> {
            val beat = max(0f, sin(t / 420)).pow(8)
            val h1 = (i * .6180339f) % 1; val h2 = (i * .4142135f) % 1; val h3 = (i * .7320508f) % 1
            val play = .5f + .5f * sin(t / (380 + h1 * 700) + h2 * 6.283f) * sin(t / (900 + h3 * 900) + h1 * 6.283f)
            val flare = max(0f, sin(t / (1700 + h2 * 2300) + h3 * 6.283f) - .93f) / .07f
            Look(1 + beat * .1f + flare * .08f, lerp(lerp(deep, pink, .35f + .65f * play), light, flare * .8f))
        }
        Fig.Ring -> {
            val n = 18; val head = (t / 1300) % 1 * n; val d = ((head - (i % n)) + n) % n
            val lit = max(0f, 1 - d / 7); val out = if (d < 1) (1 - d) * .07f else 0f
            val a = (i % n) / n.toFloat() * TWO_PI - PI.toFloat() / 2
            Look(1f, if (lit > 0) lerp(deep, pink, .3f + .7f * lit) else grey, cos(a) * out, sin(a) * out)
        }
        Fig.Tiles -> {
            val e = easeInOut(clamp01(tilesLevel(t) - (i % 36) / 4))
            val shade = .5f + .5f * sin(t / 330 + i * 2.17f)
            val lit = lerp(deep, lerp(pink, light, shade * .5f), .45f + .55f * shade)
            val dim = lerp(grey, deep, .35f + .3f * abs(sin(t / 170 + i * 2.3f)))
            Look(.62f + .26f * e, lerp(dim, lit, e))
        }
    }

    // The explosion happens in about one regroup of three, and then the flight starts straight out of it - no waiting
    // flung out (the owner, ≈ 13:05: «пиксели разбежались и зависли и стоят - бесит. Давай откажемся от обязательного
    // взрыва… Пусть они живо бегают, как раньше» · «и иногда взрываются, но без этой паузы-зависания»).
    private fun burstTime(s: Sq) = if (s.kick != null) BURST else 0f
    fun progress(s: Sq, t: Float) = clamp01((t - s.start - burstTime(s) - s.delay) / s.dur)
    private fun burstOf(s: Sq, t: Float) = if (s.kick != null) clamp01((t - s.start) / BURST) else 1f

    fun pos(s: Sq, t: Float): Pt {
        val kick = s.kick
        if (kick != null && t - s.start < BURST + s.delay) {      // flung out
            val b = easeOut(burstOf(s, t))
            return Pt(s.from.x + (kick.x - s.from.x) * b, s.from.y + (kick.y - s.from.y) * b)
        }
        val k = progress(s, t)
        val u = (if (s.who == Who.Overshooter) backOut(k) else easeInOut(k)).coerceIn(0f, 1.2f)
        val a = (1 - u) * (1 - u); val b = 2 * (1 - u) * u; val c = u * u; val o = kick ?: s.from
        var x = a * o.x + b * s.ctrl.x + c * s.to.x
        val y = a * o.y + b * s.ctrl.y + c * s.to.y
        if (s.who == Who.Overshooter && k >= 1) x += sin((t - s.start) / 120) * exp(-(t - s.start - burstTime(s) - s.delay - s.dur) / 300) * .015f
        return Pt(x, y)
    }

    fun lookOf(s: Sq, i: Int, t: Float): Look {
        val live = life(fig, i, t); val k = progress(s, t)
        if (k >= 1) return live
        val e = easeInOut(k)
        val lift = max(sin(burstOf(s, t) * PI.toFloat()) * .55f, sin(e * PI.toFloat()) * .35f)
        return Look((s.look.k + (live.k - s.look.k) * e) * (1 - lift * .3f), lerp(lerp(s.look.c, live.c, e), light, lift))
    }

    private fun regroup(t: Float, next: Fig, nextMood: Mood) {
        val guests = IntArray(N)
        val explode = rnd.nextFloat() < EXPLODE_SHARE
        for (i in 0 until N) {
            val s = sq[i]; val p = pos(s, t)
            s.look = lookOf(s, i, t)                              // leave as it looks now
            s.from = p; s.start = t; s.host = -1; s.splitAt = if (s.merged) t else -1f; s.merged = false
            if (i < next.count) s.to = next.target(i) else {      // a spare one merges into a near, uncrowded host
                var best = 0; var bestCost = Float.MAX_VALUE
                for (j in 0 until next.count) {
                    val q = next.target(j); val cost = hypot(q.x - p.x, q.y - p.y) + guests[j] * .35f
                    if (cost < bestCost) { bestCost = cost; best = j }
                }
                guests[best]++; s.host = best; s.to = next.target(best)
            }
            s.who = Who.entries[rnd.nextInt(Who.entries.size)]
            val ang = atan2(p.y - .5f, p.x - .5f) + (rnd.nextFloat() - .5f) * 1.1f; val kick = .1f + rnd.nextFloat() * .1f
            val kp = if (explode) Pt(p.x + cos(ang) * kick, p.y + sin(ang) * kick) else p; s.kick = if (explode) kp else null
            s.ctrl = if (s.who == Who.Straight) Pt((kp.x + s.to.x) / 2, (kp.y + s.to.y) / 2) else when (nextMood) {
                Mood.Burst -> Pt(.5f + (kp.x - .5f) * 3, .5f + (kp.y - .5f) * 3)
                Mood.Swirl -> { val a = atan2(kp.y - .5f, kp.x - .5f) + 2.2f; Pt(.5f + cos(a) * .55f, .5f + sin(a) * .55f) }
                Mood.Rain -> Pt(s.to.x + (rnd.nextFloat() - .5f) * .2f, -.45f)
                Mood.Snake -> Pt((kp.x + s.to.x) / 2 + (rnd.nextFloat() - .5f) * .5f, (kp.y + s.to.y) / 2 + (rnd.nextFloat() - .5f) * .5f)
            }
            // Quicker since the owner's «переходы быстрее, сейчас пиксели долго "думают"» (≈ 13:01): delays about halved.
            val moodDelay = when (nextMood) { Mood.Burst -> rnd.nextFloat() * 120; Mood.Swirl -> rnd.nextFloat() * 150
                Mood.Rain -> rnd.nextFloat() * 300; Mood.Snake -> i * 25f }
            s.delay = if (explode) rnd.nextFloat() * 60 else                        // out of the blast straight into the flight
                (if (next.stagger > 0) (1 - s.to.y) * next.stagger else moodDelay) + if (s.who == Who.Shy) 150 + rnd.nextFloat() * 150 else 0f
            s.dur = (if (s.who == Who.Shy) 450f else 600f) + rnd.nextFloat() * 300
            s.spin = if (s.who == Who.Spinner) (if (rnd.nextBoolean()) -1 else 1) * PI.toFloat() * (1 + rnd.nextInt(2)) else 0f
        }
        fig = next; mood = nextMood; formedAt = t
        switchAt = t + 2500 + rnd.nextFloat() * 2500 + next.stagger
    }

    fun start() = regroup(0f, Fig.Heart, Mood.Burst)

    /** Moves the spinner to time [t] (ms on the slowed clock): a due regroup, merges that just happened. */
    fun update(t: Float) {
        val tilesHold = tilesLevel(t) >= 10                       // the tiles square leaves only once fully built
        if (t > switchAt && (fig != Fig.Tiles || tilesHold)) {
            val figs = Fig.entries.filter { it != fig }; val moods = Mood.entries.filter { it != mood }
            regroup(t, figs[rnd.nextInt(figs.size)], moods[rnd.nextInt(moods.size)])
        }
        for (s in sq) if (s.host >= 0 && !s.merged && progress(s, t) >= 1) { s.merged = true; sq[s.host].gulpAt = t }
    }
}

/** The project's indeterminate spinner: see the file comment. Draw size is the composable's size. */
@Composable
fun PixelSpinner(modifier: Modifier = Modifier) {
    val pink = MaterialTheme.colorScheme.primary
    val grey = MaterialTheme.colorScheme.surfaceContainerHighest
    val state = remember(pink, grey) { PixelSpinnerState(pink, grey).also { it.start() } }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        val first = withFrameMillis { it }
        while (true) withFrameMillis { now -> t = (now - first) / SLOW; state.update(t) }
    }
    Canvas(modifier) {
        val now = t                                                // read in draw: only the draw phase repeats per frame
        val side0 = size.minDimension
        val ox = (size.width - side0) / 2; val oy = (size.height - side0) / 2
        for (i in 0 until N) {
            val s = state.sq[i]; val k = state.progress(s, now); val home = k >= 1
            if (s.host >= 0 && home) continue                      // merged into its host
            val p = state.pos(s, now); val l = state.lookOf(s, i, now)
            val gulp = if (s.gulpAt >= 0) max(0f, 1 - (now - s.gulpAt) / 380) else 0f
            val split = if (s.splitAt >= 0) max(0f, 1 - (now - s.splitAt - s.delay) / 300) else 0f
            val side = side0 * state.fig.size * l.k * (1 + .22f * sin(gulp * PI.toFloat())) * (1 - .3f * split)
            val cx = ox + (p.x + if (home) l.dx else 0f) * side0
            val cy = oy + (p.y + if (home) l.dy else 0f) * side0
            translate(cx, cy) {
                rotateRad(s.spin * if (home) 1f else easeInOut(k), Offset.Zero) {
                    drawRect(l.c, Offset(-side / 2, -side / 2), Size(side, side))
                }
            }
        }
    }
}
