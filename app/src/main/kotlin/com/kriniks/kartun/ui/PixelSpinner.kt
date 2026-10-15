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
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
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
 * The squares gather into a heart, a pixel ring or a square of nine tiles - and, since 2026-10-15 ≈ 17:55, into the states
 * the owner picked from design/spinner-states.html (ideas/03; «Нейросеть… Свертка… Внимание… Квадродерево… Игра в жизнь…
 * Сортировка… Фурье… Остальные не берём» · «можешь уже в коде делать»): a three-layer neural network, a 3x3 convolution,
 * attention, a quadtree of the heart, Conway's Life on 16x16 with real seeds, a bubble sort, a Fourier wave. Every 4-7 s
 * they regroup into another figure. A regroup opens with a small explosion one time in three, then each square flies by the
 * regroup's mood (burst, swirl, rain, snake, and the owner's matrix and wind) and its own character (straight, arc, shy,
 * spinner, overshooter). A figure with fewer places merges the spare squares into the nearest host; a figure that needs more
 * splits them off again. In flight a square blends from the look it left with into the new figure's look. The heart builds
 * from the bottom and then plays colours at random; the tiles square only builds and regroups once fully built; the ring runs
 * a wave; the new states live the heart's colour life with their meaning on top. Small spinners (under 40 dp: the 22 dp one on a loading model card) skip the
 * fine-grained states. All time is 1.5x slower than the first take; the new states run their own steps on real time.
 * [TESTED: 2026-10-15 · seen on the Titan, debug probe screen at 150 dp: ~100 frames over two minutes show heart, ring,
 * tiles, net with links and sparks, convolution, attention, Life (pulsar, glider, spaceship, pentadecathlon), sort, wave,
 * the regroups between them, no crash; the owner watched it live - «да, анимация очень красивая стала». NOT walked: the
 * 48 dp processing screen and the 22 dp model card (compact set), the quadtree seen only in a moment. The 18:20-18:27
 * tweaks - quadtree grid and pixel ends, the gliding convolution window without placeholders, the smart random - built,
 * installed, the app alive and computing; their look NOT yet observed by the agent]
 */

private const val SLOW = 1.5f
private const val BURST = 380f
/** The share of regroups that open with the small explosion. */
private const val EXPLODE_SHARE = .33f
private const val TWO_PI = (2 * PI).toFloat()
private const val PI_F = PI.toFloat()

private fun clamp01(v: Float) = v.coerceIn(0f, 1f)
private fun easeInOut(t: Float) = if (t < .5f) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
private fun easeOut(t: Float) = 1 - (1 - t).pow(3)
private fun backOut(t: Float): Float { val s = 1.4f; return 1 + (s + 1) * (t - 1).pow(3) + s * (t - 1).pow(2) }
private fun mixF(a: Float, b: Float, k: Float) = a + (b - a) * k
private fun frac(v: Float) = v - floor(v)
/** A fixed pseudo-random number in 0..1 for a pair of integers: link weights, input patterns, attention scores. */
private fun hash01(a: Int, b: Int): Float { val v = sin(a * 12.9898f + b * 78.233f) * 43758.547f; return v - floor(v) }
/** sin over the window [a, b] of x: 0 outside, 1 in the middle - when something is lit within one cycle. */
private fun bump(x: Float, a: Float, b: Float) = sin(PI_F * clamp01((x - a) / (b - a)))

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

/*
 * The neural-network figure (the owner, chat 2026-10-15 ≈ 17:25 +03:00: «пиксели выстраиваются в три слоя deep neural network,
 * между ними появляются едва заметные линии-связи, пиксели пульсируют цветами - передают друг другу потенциалы возбуждения»;
 * ≈ 17:45: «пусть пиксели чуть-чуть покачиваются, и меняются в размерах, и сильнее меняются в цвете, переливаются, как в нашем
 * сердце»; ≈ 17:50: «такого чуть-чуть хватит»). Three layers of 5, 7 and 5 neurons; every NET_WAVE ms an input pattern lights
 * the first layer and the excitation runs along the links to the next layers, each neuron lighting by the weighted sum of
 * what reaches it.
 */
private val NET_LAYERS = intArrayOf(5, 7, 5)
private val NET_LAYER_OF = NET_LAYERS.flatMapIndexed { l, n -> List(n) { l } }
private val NET_PTS = NET_LAYERS.flatMapIndexed { l, n ->
    List(n) { j -> Pt(.18f + l * .32f, .5f + (j - (n - 1) / 2f) * .7f / 6) }
}
private val NET_EDGES = buildList {
    var base = 0
    for (l in 0 until NET_LAYERS.size - 1) {
        val next = base + NET_LAYERS[l]
        for (a in base until next) for (b in next until next + NET_LAYERS[l + 1]) add(a to b)
        base = next
    }
}
private const val NET_WAVE = 1800f

/*
 * Convolution (≈ 17:45: «Свертка: быстрее, и пусть не линия порождает сверточный пиксель, а пусть он выпрыгиват из окна и
 * становится в свертку»): 25 input squares (5x5) and 9 output squares (3x3). A 3x3 kernel window stops at each of the 9
 * places; at each stop the result pixel jumps out of the window into its place in the output; then the output glows.
 */
private const val CONV_STEP = 380f
private const val CONV_STEPS = 13
private fun convIn(x: Int, y: Int) = .2f + .8f * hash01(x + 1, y + 7)
private val CONV_OUT = FloatArray(9) { j -> var s = 0f; for (a in 0..2) for (b in 0..2) s += convIn(j % 3 + a, j / 3 + b); s / 9 }
private fun convWindow(pos: Int) = Pt(.2f + (pos % 3) * .1f, .4f + (pos / 3) * .1f)

/*
 * Attention (≈ 17:45: «Внимание: бсытрее, веселее, переливы, можно линии не делать»; ≈ 17:50: «в внимание не должно столбцы
 * тоже перебирать?»): a 6x6 table query x key, 6 query squares on the left, 6 key squares on top. Each query in turn runs a
 * light along the keys of its row, then its row pops by the softmax weights; earlier rows keep an afterglow.
 */
private const val ATTN_STEP = 720f
private fun attnWeight(row: Int, round: Int, c: Int): Float {
    var sum = 0f
    for (k in 0..5) sum += exp(3.2f * hash01(row * 7 + round * 13, k))
    return exp(3.2f * hash01(row * 7 + round * 13, c)) / sum
}

/*
 * Quadtree (≈ 17:45: «Квадродерево: быстрее, веселее, переливы»; ≈ 17:50: «квадродерево какое-то страшное получилось»): the
 * quadtree of an 8x8 heart picture, as image codecs build it - a block that is all heart or all background stays one square,
 * a block an edge runs through splits in four, down to single pixels; then it merges back.
 */
private val QUAD_IMG = listOf("00000000", "01100110", "11111111", "11111111", "01111110", "00111100", "00011000", "00000000")
private class Block(val x: Float, val y: Float, val side: Int, val v: Float, val key: Int)
private fun quadBlock(cell: Int, level: Int): Block {
    val cx = cell % 8; val cy = cell / 8
    // level 4 is the picture itself, every pixel apart - the quadtree's finest level keeps uniform blocks whole, and those
    // flew as big squares in and out of the figure
    if (level >= 4) return Block(.1f + (cx + .5f) * .1f, .1f + (cy + .5f) * .1f, 1, if (QUAD_IMG[cy][cx] == '1') 1f else 0f, 64 + cy * 8 + cx)
    var b = 8; var depth = 0
    while (true) {
        val bx = cx / b * b; val by = cy / b * b
        var ones = 0
        for (y in by until by + b) for (x in bx until bx + b) if (QUAD_IMG[y][x] == '1') ones++
        if (ones == 0 || ones == b * b || depth >= level || b == 1)
            return Block(.1f + (bx + b / 2f) * .1f, .1f + (by + b / 2f) * .1f, b, ones / (b * b).toFloat(), b * 64 + by * 8 + bx)
        b /= 2; depth++
    }
}
private val QUAD_LUT = Array(5) { level -> Array(64) { cell -> quadBlock(cell, level) } }
private const val QUAD_CYCLE = 4400f
/** The gap between quadtree pixels and blocks, in cells (the heart's is about 12 % of its step). */
private const val QUAD_GAP = .16f

/*
 * Conway's Game of Life (≈ 17:45: «Игра в жизнь: больше размерность сетки, быстрее, и сидить реальными сидами, чтобы там
 * рально считались интересные узнаваемые паттерны, и переливы цветов, быстрее»): a 16x16 torus, seeded in turn with four
 * gliders, the pulsar, a lightweight spaceship, the pentadecathlon and the R-pentomino.
 */
private const val LIFE_G = 16
private const val LIFE_CELL = .9f / LIFE_G
private const val LIFE_STEP = 110f
private class Seed(val ms: Float, val cells: List<Int>)
private fun lifeRows(r: List<String>, ox: Int, oy: Int) = r.flatMapIndexed { y, s ->
    s.mapIndexedNotNull { x, ch -> if (ch == 'O') ((y + oy) % LIFE_G) * LIFE_G + (x + ox) % LIFE_G else null }
}
private val GLIDER = listOf(".O.", "..O", "OOO")
private val PULSAR = buildList {
    for (r in intArrayOf(0, 5, 7, 12)) for (c in intArrayOf(2, 3, 4, 8, 9, 10)) add((r + 1) * LIFE_G + c + 1)
    for (r in intArrayOf(2, 3, 4, 8, 9, 10)) for (c in intArrayOf(0, 5, 7, 12)) add((r + 1) * LIFE_G + c + 1)
}
private val LIFE_SEEDS = listOf(
    Seed(5200f, lifeRows(GLIDER, 1, 1) + lifeRows(GLIDER, 9, 2) + lifeRows(GLIDER, 2, 9) + lifeRows(GLIDER, 10, 10)),
    Seed(4000f, PULSAR),
    Seed(4800f, lifeRows(listOf(".O..O", "O....", "O...O", "OOOO."), 2, 6)),
    Seed(4400f, lifeRows(listOf("..O....O..", "OO.OOOO.OO", "..O....O.."), 3, 7)),
    Seed(7000f, lifeRows(listOf(".OO", "OO.", ".O."), 7, 7)),
)
private val LIFE_TOTAL = LIFE_SEEDS.fold(0f) { s, x -> s + x.ms }
private fun lifeStep(c: BooleanArray) = BooleanArray(c.size) { i ->
    val x = i % LIFE_G; val y = i / LIFE_G; var n = 0
    for (a in -1..1) for (b in -1..1) if ((a != 0 || b != 0) && c[((y + b + LIFE_G) % LIFE_G) * LIFE_G + (x + a + LIFE_G) % LIFE_G]) n++
    n == 3 || (c[i] && n == 2)
}

/*
 * Sorting (≈ 17:45: «Сортировка: веселее, быстрее, переливы»): 8 columns of 1..8 squares - 36 in all; a bubble sort, the
 * swapped columns hop over each other; a victory wave when sorted, then the columns jump into the next shuffle.
 */
private const val SORT_STEP = 120f
private const val SORT_CMP = 28
private const val SORT_WAVE = 900f
private const val SORT_HOP = 450f
private const val SORT_ROUND = SORT_CMP * SORT_STEP + SORT_WAVE + SORT_HOP
private val SORT_HK = buildList { for (h in 1..8) for (k in 0 until h) add(h to k) }
private fun sortColX(c: Int) = .12f + c * .108f
private fun sortPerm(round: Int): IntArray {
    val a = IntArray(8) { it + 1 }
    for (i in 7 downTo 1) { val j = (hash01(round, i) * (i + 1)).toInt().coerceAtMost(i); val x = a[i]; a[i] = a[j]; a[j] = x }
    return a
}

// Fourier (≈ 17:45: «Фурье: быстрее, веселее, переливы»): 36 squares draw the sum of 1..4 harmonics of a square wave.
private const val WAVE_CYCLE = 4500f

private enum class Fig(val size: Float, val count: Int, val stagger: Float, val hold: Float, val fine: Boolean = false) {
    Heart(.1f, HEART_PTS.size, 1000f, 0f), Ring(.085f, 18, 0f, 0f), Tiles(TILE_SUB, 36, 0f, 0f),
    Net(.075f, NET_PTS.size, 0f, 3500f), Conv(.085f, 34, 0f, 3400f, true), Attn(.1f, 48, 0f, 2600f, true),
    Quad(.1f, 64, 0f, 1800f, true), Life(LIFE_CELL, LIFE_G * LIFE_G, 0f, 4000f, true), Sort(.085f, 36, 0f, 3200f),
    Wave(.042f, 36, 0f, 1500f);

    fun target(i: Int): Pt = when (this) {
        Heart -> HEART_PTS[i]
        Ring -> { val a = i / 18f * TWO_PI - PI_F / 2; Pt(.5f + cos(a) * .36f, .5f + sin(a) * .36f) }
        Tiles -> {
            val j = i % 36; val tile = j / 4; val sub = j % 4; val tx = tile % 3; val ty = tile / 3
            Pt(SQ0 + tx * (TILE + SEAM) + (sub % 2 + .5f) * TILE_SUB, SQ0 + ty * (TILE + SEAM) + (sub / 2 + .5f) * TILE_SUB)
        }
        Net -> NET_PTS[i]
        Conv -> if (i < 25) Pt(.1f + (i % 5) * .1f, .3f + (i / 5) * .1f) else Pt(.72f + ((i - 25) % 3) * .1f, .4f + ((i - 25) / 3) * .1f)
        Attn -> when {
            i < 36 -> Pt(.22f + (i % 6) * .12f, .22f + (i / 6) * .12f)
            i < 42 -> Pt(.08f, .22f + (i - 36) * .12f)
            else -> Pt(.22f + (i - 42) * .12f, .08f)
        }
        Quad -> Pt(.1f + (i % 8 + .5f) * .1f, .1f + (i / 8 + .5f) * .1f)
        Life -> Pt(.05f + (i % LIFE_G + .5f) * LIFE_CELL, .05f + (i / LIFE_G + .5f) * LIFE_CELL)
        Sort -> { val (h, k) = SORT_HK[i]; Pt(sortColX(h - 1), .88f - k * .095f) }
        Wave -> Pt(.07f + i / 35f * .86f, .5f)
    }
}

/** Weights of the smart random of figures: just shown, the gain per regroup not chosen, the cap. */
private const val FIG_W_SHOWN = .15f
private const val FIG_W_GROW = .5f
private const val FIG_W_MAX = 4f

/** As many squares as the largest figure needs; the others merge the spare ones into hosts. */
private val N = Fig.entries.maxOf { it.count }

// Matrix and Wind are the owner's (≈ 17:47: «мне нравится, что у тебя разные переходы, взрыв, торнадо, дождь, матрица (не
// уверен, есть ли), ветер»): Matrix drops the squares column after column from above; Wind blows them off to the right and
// brings them back to their places.
private enum class Mood { Burst, Swirl, Rain, Snake, Matrix, Wind }
private enum class Who { Straight, Arc, Shy, Spinner, Overshooter }

private class Sq(rnd: Random) {
    var from = Pt(rnd.nextFloat(), rnd.nextFloat()); var kick: Pt? = null; var ctrl = Pt(.5f, .5f); var to = Pt(.5f, .5f)
    var start = 0f; var delay = 0f; var dur = 900f; var who = Who.Straight; var spin = 0f
    var host = -1; var merged = false; var splitAt = -1f; var gulpAt = -1f
    /** Needed by neither the figure it leaves nor the next one: it waits inside a host, unseen. */
    var hidden = false
    var look = Look(.8f, Color.Unspecified)
}

/** All the spinner's state; [update] moves it on, [draw] only reads it. */
private class PixelSpinnerState(private val pink: Color, private val grey: Color) {
    private val deep = lerp(pink, Color.Black, .53f)
    private val light = lerp(pink, Color.White, .45f)
    private val plum = lerp(pink, Color.Black, .8f)
    private val rnd = Random(System.nanoTime())
    val sq = Array(N) { Sq(rnd).also { it.look = Look(.8f, deep) } }
    var fig = Fig.Heart; private set
    private var mood = Mood.Burst
    private var switchAt = 0f
    private var formedAt = 0f
    /** Set by the draw from the spinner's size: a small spinner skips the fine-grained states. */
    var compact = false

    private fun tilesLevel(t: Float) = ((t - formedAt - 700) / 260).coerceIn(0f, 10f)

    /** Real milliseconds since the figure formed, for the states that run their own steps (0 while the squares fly in). */
    private fun scene(t: Float) = max(0f, (t - formedAt - 700) * SLOW)

    /** The heart's colour life (Fig.Heart below) for square [i], lit by a state's meaning [v]: 0 rests dark, 1 burns light. */
    private fun glow(i: Int, t: Float, v: Float): Look {
        val beat = max(0f, sin(t / 420)).pow(8)
        val h1 = (i * .6180339f) % 1; val h2 = (i * .4142135f) % 1; val h3 = (i * .7320508f) % 1
        val play = .5f + .5f * sin(t / (380 + h1 * 700) + h2 * 6.283f) * sin(t / (900 + h3 * 900) + h1 * 6.283f)
        val flare = max(0f, sin(t / (1700 + h2 * 2300) + h3 * 6.283f) - .93f) / .07f
        val rest = lerp(lerp(plum, deep, play), pink, .2f * play); val hot = lerp(pink, light, .25f + .6f * play)
        return Look(1 + beat * .1f + flare * .08f, lerp(lerp(rest, hot, clamp01(v)), light, flare * .8f))
    }

    /** Now and then a square sways a little - a rare per-square event, shaped like the heart's flare (the owner, ≈ 17:50:
     *  «иногда что-то из них покачивается: такого чуть-чуть хватит»). */
    private fun swayed(l: Look, i: Int, t: Float, a: Float): Look {
        val h1 = (i * .618f) % 1; val h2 = (i * .414f) % 1; val h3 = (i * .732f) % 1
        val ev = max(0f, sin(t / (1530 + h1 * 1730) + h3 * 6.283f) - .8f) / .2f
        if (ev <= 0f) return l
        return Look(l.k, l.c, l.dx + a * ev * sin(t / 93 + h2 * 6.283f), l.dy + a * ev * .5f * cos(t / 113 + h1 * 6.283f))
    }

    // ---- the neural network

    /** Time since the net formed; the first wave starts once the neurons have flown in. */
    private fun netClock(t: Float) = t - formedAt - 1100

    /** Activation 0..1 of each neuron in wave [w]: inputs from the pattern, deeper layers by the weighted sums. */
    private fun netActs(w: Int): FloatArray {
        val act = FloatArray(NET_PTS.size)
        for (i in act.indices) if (NET_LAYER_OF[i] == 0) act[i] = hash01(w, i).pow(2)
        var base = 0
        for (l in 0 until NET_LAYERS.size - 1) {
            val next = base + NET_LAYERS[l]
            for (b in next until next + NET_LAYERS[l + 1]) {
                var sum = 0f
                for (a in base until next) sum += hash01(a, b) * act[a]
                act[b] = clamp01(sum / NET_LAYERS[l] * 2.2f).pow(1.5f)
            }
            base = next
        }
        return act
    }

    private var actsWave = Int.MIN_VALUE
    private var acts = FloatArray(NET_PTS.size)
    private fun actsAt(t: Float): FloatArray {
        val w = floor(netClock(t) / NET_WAVE).toInt()
        if (w != actsWave) { actsWave = w; acts = netActs(w) }
        return acts
    }

    /** The wave's phase 0..1, or -1 before the first wave. */
    private fun netPhase(t: Float): Float { val c = netClock(t); return if (c < 0) -1f else (c % NET_WAVE) / NET_WAVE }

    private fun netWaveNo(t: Float) = floor(netClock(t) / NET_WAVE).toInt()

    /** The spark of link (a → b) in wave [w]: when it leaves (phase), how long it runs, how strong it is; null when the link
     *  stays quiet this wave. Each link in each wave has its own start, speed and a chance to stay silent (the owner, ≈ 18:08:
     *  «чтобы потенциалы по проводам бежали немного случайнее, хаотичнее»). */
    private fun spark(a: Int, b: Int, w: Int, act: FloatArray): Triple<Float, Float, Float>? {
        if (hash01(a + b * 3, w + 11) < .25f) return null          // a quarter of the links stay quiet this wave
        val start = NET_LAYER_OF[a] * .3f + .06f + hash01(a * 31 + b, w) * .2f
        return Triple(start, .16f + hash01(b * 17 + a, w + 7) * .26f, act[a] * (.4f + .6f * hash01(a, b)))
    }

    /** How lit neuron [i] is now, over a faint idle glow. An input neuron glows by its activation in its window of the wave;
     *  a deeper one flashes when sparks reach it, by their strength, and fades (the owner, ≈ 18:10: «когда потенциалы в
     *  нейросети добегают до нейронов-пикселей, они должны загораться»). */
    private fun netLit(i: Int, t: Float): Float {
        val ph = netPhase(t); if (ph < 0) return .12f
        val w = netWaveNo(t); val act = actsAt(t)
        if (NET_LAYER_OF[i] == 0) {
            val start = (hash01(i * 5 + 1, w) - .5f) * .1f
            return .12f + .88f * act[i] * bump(ph, start, start + .4f)
        }
        var flash = 0f
        for ((a, b) in NET_EDGES) {
            if (b != i) continue
            val (start, dur, strength) = spark(a, b, w, act) ?: continue
            val since = ph - (start + dur)
            if (since >= 0f) flash += strength * exp(-since / .07f)
        }
        return .12f + .88f * clamp01(flash * 1.4f)
    }

    /** Link (a → b) now: how bright the line is (0..1) and where its travelling spark is (0..1, or -1 when none). */
    fun netLink(a: Int, b: Int, t: Float): Pair<Float, Float> {
        val ph = netPhase(t); if (ph < 0) return 0f to -1f
        val (start, dur, strength) = spark(a, b, netWaveNo(t), actsAt(t)) ?: return 0f to -1f
        val k = (ph - start) / dur
        return if (k in 0f..1f) strength * sin(PI_F * k) to k else 0f to -1f
    }

    /** 0..1: the links fade in once the neurons stand in their layers. */
    fun netLinks(t: Float) = if (fig == Fig.Net) clamp01((t - formedAt - 900) / 800) else 0f

    // ---- Life: the board of the current seed, stepped on from its seed and cached per generation

    private var lifeSeedBase = 0
    private var lifeKey = -1
    private var lifeGen = 0
    private var lifeCur = BooleanArray(LIFE_G * LIFE_G)
    private var lifePrev = lifeCur
    private var lifeF = 0f

    private fun lifeAt(t: Float) {
        val r = scene(t); var rr = r % LIFE_TOTAL; var idx = 0
        while (rr >= LIFE_SEEDS[(idx + lifeSeedBase) % LIFE_SEEDS.size].ms) { rr -= LIFE_SEEDS[(idx + lifeSeedBase) % LIFE_SEEDS.size].ms; idx++ }
        val key = (r / LIFE_TOTAL).toInt() * LIFE_SEEDS.size + idx
        val gen = (rr / LIFE_STEP).toInt(); lifeF = frac(rr / LIFE_STEP)
        if (key != lifeKey || gen < lifeGen) {
            val c = BooleanArray(LIFE_G * LIFE_G)
            for (cell in LIFE_SEEDS[(idx + lifeSeedBase) % LIFE_SEEDS.size].cells) c[cell] = true
            lifeKey = key; lifeGen = 0; lifeCur = c; lifePrev = c
        }
        while (lifeGen < gen) { lifePrev = lifeCur; lifeCur = lifeStep(lifeCur); lifeGen++ }
    }

    // ---- sorting: the columns now, worked out once per frame

    private var sortT = Float.NaN
    private val sortColOf = IntArray(9)            // value 1..8 -> its column now
    private val sortNextColOf = IntArray(9)        // value -> its column in the next round's shuffle
    private var sortCmp = -1; private var sortSwap = false; private var sortF = 0f
    private var sortWaveT = -1f; private var sortHopT = -1f

    private fun sortAt(t: Float) {
        if (t == sortT) return
        sortT = t
        val r = scene(t); val round = (r / SORT_ROUND).toInt(); val tau = r % SORT_ROUND
        val b = sortPerm(round); val idx = min(SORT_CMP, (tau / SORT_STEP).toInt()); sortF = frac(tau / SORT_STEP)
        sortCmp = -1; sortSwap = false
        var count = 0
        run passes@{
            for (i in 0 until 7) for (j in 0 until 7 - i) {
                if (count == idx) { sortCmp = j; sortSwap = b[j] > b[j + 1]; return@passes }
                if (b[j] > b[j + 1]) { val x = b[j]; b[j] = b[j + 1]; b[j + 1] = x }
                count++
            }
        }
        for (c in 0 until 8) sortColOf[b[c]] = c
        val nb = sortPerm(round + 1); for (c in 0 until 8) sortNextColOf[nb[c]] = c
        sortWaveT = tau - SORT_CMP * SORT_STEP; sortHopT = sortWaveT - SORT_WAVE
    }

    /** The quadtree's depth now, 4 (every pixel apart) .. 0 (one block): a cycle starts and ends at the pixels and merges up to
     *  the big blocks in its middle, so the squares fly in and out small (the owner, ≈ 18:14: «только немножко страшные
     *  большие пиксели, когда квадро дерево переход делает в и из»). */
    private fun quadLevel(t: Float): Float {
        val ph = frac(scene(t) / QUAD_CYCLE)
        return 4 - easeInOut(if (ph < .5f) ph * 2 else (1 - ph) * 2) * 4
    }

    // ---- the life of a square that stands in figure f

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
            val a = (i % n) / n.toFloat() * TWO_PI - PI_F / 2
            Look(1f, if (lit > 0) lerp(deep, pink, .3f + .7f * lit) else grey, cos(a) * out, sin(a) * out)
        }
        Fig.Tiles -> {
            val e = easeInOut(clamp01(tilesLevel(t) - (i % 36) / 4))
            val shade = .5f + .5f * sin(t / 330 + i * 2.17f)
            val lit = lerp(deep, lerp(pink, light, shade * .5f), .45f + .55f * shade)
            val dimTile = lerp(grey, deep, .35f + .3f * abs(sin(t / 170 + i * 2.3f)))
            Look(.62f + .26f * e, lerp(dimTile, lit, e))
        }
        Fig.Net -> {
            val lit = netLit(i % NET_PTS.size, t); val g = glow(i, t, .15f + .85f * lit)
            swayed(Look(g.k * (.94f + .12f * lit), g.c), i, t, .007f)
        }
        Fig.Conv -> convLife(i % 34, t)
        Fig.Attn -> attnLife(i % 48, t)
        Fig.Quad -> {
            val lvl = quadLevel(t)
            val lo = min(4, lvl.toInt()); val hi = min(4, lo + 1); val fr = easeInOut(lvl - lo)
            val j = i % 64; val a = QUAD_LUT[lo][j]; val b = QUAD_LUT[hi][j]; val tg = Fig.Quad.target(j)
            val g = glow(if (fr < .5f) a.key else b.key, t, .15f + .75f * mixF(a.v, b.v, fr))
            // a fixed gap between pixels and blocks, a grid like the heart's (the owner, ≈ 18:20: «пусть у квадродерева
            // между пикселями будет промежуток сетка небольшая» · «как в сердчке»): side = cells - QUAD_GAP
            Look((mixF(a.side.toFloat(), b.side.toFloat(), fr) - QUAD_GAP) * g.k, g.c, mixF(a.x, b.x, fr) - tg.x, mixF(a.y, b.y, fr) - tg.y)
        }
        Fig.Life -> {
            lifeAt(t); val j = i % (LIFE_G * LIFE_G)
            when {
                lifeCur[j] -> { val g = glow(j, t, .6f + .4f * sin(scene(t) / 250 + j)); Look(.86f * g.k, if (!lifePrev[j]) lerp(light, g.c, lifeF) else g.c) }
                // the empty board stays visible grey, like the ring's unlit squares: squares land on it and take off from
                // it (near-black, they seemed to vanish into nothing and come out of nothing - the owner, ≈ 18:05: «пиксели
                // словно из ниоткуда появляются и в никуда исчезают - раньше было, что они делились, или схлопывались»)
                lifePrev[j] -> Look(mixF(.86f, .55f, lifeF), lerp(deep, grey, lifeF))
                else -> Look(.55f, grey)
            }
        }
        Fig.Sort -> sortLife(i % 36, t)
        Fig.Wave -> {
            val r = scene(t); val ph = frac(r / WAVE_CYCLE); val terms = 1 + (if (ph < .5f) ph * 2 else (1 - ph) * 2) * 3
            val j = i % 36; val a = j / 35f * PI_F * 4 + r / 260; var y = 0f
            for (k in 0..3) y += clamp01(terms - k) * sin((2 * k + 1) * a) / (2 * k + 1)
            val g = glow(j, t, .3f + .7f * abs(y))
            Look(g.k * (1 + .1f * abs(sin(a * 2))), g.c, 0f, -y * .32f)
        }
    }

    private fun convLife(j: Int, t: Float): Look {
        val r = scene(t); val step = (r / CONV_STEP).toInt() % CONV_STEPS; val f = frac(r / CONV_STEP)
        if (j < 25) {
            val x = j % 5; val y = j / 5
            // No frame: the pixels under the kernel light up and grow (the owner, ≈ 17:58: «в свертке можно окно не делать, а
            // пусть в оригинальной группе просто увеличиваются и подсвечиваются группы 3-3»). The kernel GLIDES from stop
            // to stop over the first 45 % of a step, and each pixel lights by how much of it the gliding window covers -
            // a smooth pass, lighting up ahead and fading behind (≈ 18:08: «плавно вспыхивать и увеличиваться… а у тебя
            // резко»; ≈ 18:24: «квадрат сверточного окна всё так же резко ходит по оригиналу, не видно плавного загорания
            // и потухания и перехода» - a 90 ms switch still read as a jump). The window fades in at the first stop and out
            // after the last; +10 % keeps the nine apart.
            val glide = easeInOut(clamp01(f / .45f))
            val cur = min(step, 8); val prev = if (step in 1..8) step - 1 else cur
            val wx = mixF((prev % 3).toFloat(), (cur % 3).toFloat(), glide); val wy = mixF((prev / 3).toFloat(), (cur / 3).toFloat(), glide)
            val amp = when { step == 0 -> glide; step <= 8 -> 1f; step == 9 -> 1 - glide; else -> 0f }
            val cover = clamp01(min(x + 1f, wx + 3) - max(x.toFloat(), wx)) * clamp01(min(y + 1f, wy + 3) - max(y.toFloat(), wy))
            val u = easeInOut(cover) * amp
            val g = glow(j, t, mixF(.12f * convIn(x, y), .5f + .5f * convIn(x, y), u))
            return swayed(Look(g.k * (1 + .1f * u), g.c), j, t, .003f)
        }
        val o = j - 25
        // A result pixel not yet computed waits hidden on the centre pixel of its window, looking exactly like it - no grey
        // placeholders (the owner, ≈ 18:21: «пусть свертка когда собирает свертку 3 на 3 - не будет серых плейсхолдеров в
        // свертке, а она собирается теми, что прилетают» · «а то там уже стоит заготовка серая 3 на 3»); in the last step of
        // the round the output pixels fly back into their windows.
        val w = convWindow(o); val slot = Fig.Conv.target(j)
        val inner = convLife((o / 3 + 1) * 5 + o % 3 + 1, t)
        return when {
            step == CONV_STEPS - 1 -> {
                val e = easeInOut(f); val g = glow(j, t, .55f + .45f * CONV_OUT[o])
                Look(mixF(g.k, inner.k, e), lerp(g.c, inner.c, e), (w.x - slot.x) * e + inner.dx * e, (w.y - slot.y) * e + inner.dy * e - .1f * sin(PI_F * e))
            }
            step >= 9 || o < step -> glow(j, t, .55f + .45f * CONV_OUT[o] + (if (step >= 9) .3f * bump(frac(r / (3 * CONV_STEP)), 0f, 1f) else 0f))
            o > step -> Look(inner.k, inner.c, w.x - slot.x + inner.dx, w.y - slot.y + inner.dy)
            else -> {                                              // jumping out of the window into its place
                // it jumps once the gliding window has arrived (the glide takes the first 45 % of the step)
                val e = easeInOut(clamp01((f - .4f) / .55f)); val g = glow(j, t, 1f)
                Look(mixF(inner.k, g.k, easeInOut(clamp01((f - .4f) / .25f))), lerp(inner.c, lerp(light, g.c, e), clamp01((f - .4f) / .15f)),
                    (w.x - slot.x) * (1 - e), (w.y - slot.y) * (1 - e) - .16f * sin(PI_F * e))
            }
        }
    }

    private fun attnLife(j: Int, t: Float): Look {
        val r = scene(t); val n = (r / ATTN_STEP).toInt(); val q = n % 6; val f = frac(r / ATTN_STEP); val round = n / 6
        val scanning = f < .5f; val cursor = min(5, (f / .5f * 6).toInt())
        return when {
            j < 36 -> {
                val row = j / 6; val c = j % 6; val age = (q - row + 6) % 6; val rr = if (age > q) round - 1 else round
                val w = attnWeight(row, rr, c) * 2.6f
                var pop = 1f
                val v = when {
                    age != 0 -> w * max(0f, 1 - (age - 1 + f) / 4)
                    scanning -> if (c == cursor) 1f else if (c < cursor) .25f * w + .1f else .06f
                    else -> { pop = 1 + .35f * min(1f, w) * bump((f - .5f) / .5f, 0f, .7f); w }
                }
                val g = glow(j, t, clamp01(v + .06f))
                swayed(Look(g.k * pop, g.c), j, t, .004f)
            }
            j < 42 -> { val g = glow(j, t, if (j - 36 == q) 1f else .15f); Look(.6f * g.k, g.c) }
            else -> { val c = j - 42; val g = glow(j, t, if (scanning && c == cursor) 1f else .15f + .3f * hash01(round, c)); Look(.6f * g.k, g.c) }
        }
    }

    private fun sortLife(j: Int, t: Float): Look {
        sortAt(t)
        val (h, _) = SORT_HK[j]; val c = sortColOf[h]
        var x = sortColX(c); var hop = 0f; var v = .25f + .5f * h / 8
        if (sortCmp >= 0 && (c == sortCmp || c == sortCmp + 1)) {
            v = 1f
            if (sortSwap) {
                val e = easeInOut(sortF); val dir = if (c == sortCmp) 1 else -1
                x += dir * .108f * e; hop = .09f * sin(PI_F * e) * (if (dir > 0) 1f else .4f)
            }
        }
        if (sortWaveT >= 0 && sortHopT < 0) v = max(v, bump(sortWaveT / SORT_WAVE * 1.6f - c / 8f * .6f, 0f, .5f))
        if (sortHopT >= 0) {                                      // jump into the next shuffle
            val e = clamp01(sortHopT / SORT_HOP)
            hop = .25f * sin(PI_F * e); x = mixF(sortColX(c), sortColX(sortNextColOf[h]), easeInOut(e))
        }
        val g = glow(j, t, v); val tg = Fig.Sort.target(j)
        return swayed(Look(g.k, g.c, x - tg.x, -hop), j, t, .002f)
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

    /** How square [s] looks now: the live look of its figure, blended in flight from the look it left with - colour and size
     *  smoothly (the owner, ≈ 17:50: «пусть плавно меняют цвета с серого потухшего, на розовые»), the figure's own offsets
     *  fading in with the flight. */
    fun lookOf(s: Sq, i: Int, t: Float): Look {
        val live = life(fig, i, t); val k = progress(s, t)
        if (k >= 1) return live
        val e = easeInOut(k)
        val lift = max(sin(burstOf(s, t) * PI_F) * .55f, sin(e * PI_F) * .35f)
        return Look((s.look.k + (live.k - s.look.k) * e) * (1 - lift * .3f), lerp(lerp(s.look.c, live.c, e), light, lift),
            live.dx * e, live.dy * e)
    }

    private fun regroup(t: Float, next: Fig, nextMood: Mood) {
        val guests = IntArray(N)
        val explode = rnd.nextFloat() < EXPLODE_SHARE
        if (next == Fig.Life) lifeSeedBase = (lifeSeedBase + 1) % LIFE_SEEDS.size
        // Only the squares one of the two figures needs fly; the rest stay hidden inside hosts (the owner, ≈ 18:03: «в
        // анимациях переходов как-то очень много пикселей стало, не?» - all 256 flew in every regroup since Life came in).
        val active = max(fig.count, next.count)
        for (i in 0 until N) {
            val s = sq[i]
            if (i >= active) {
                val h = i % next.count; val tg = next.target(h)
                s.hidden = true; s.merged = true; s.host = h; s.from = tg; s.to = tg; s.ctrl = tg; s.kick = null
                s.start = t; s.delay = 0f; s.dur = 1f; s.splitAt = -1f
                continue
            }
            s.hidden = false
            val now = lookOf(s, i, t); val raw = pos(s, t)
            // Leave from where it is SEEN - the path plus the figure's offsets - and as it looks now. The drawn side is
            // fig.size * look.k and fig switches to `next` right here, so the look's k moves into the new figure's unit -
            // else every square jumped in size as the regroup began (the tiles square most: its cells are the largest; the
            // owner, ≈ 17:53: «"Квадрат" в момент перед анимацией перехода заметно дергается»).
            val p = Pt(raw.x + now.dx, raw.y + now.dy)
            s.look = Look(now.k * fig.size / next.size, now.c)
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
            val keepsMood = nextMood == Mood.Matrix || nextMood == Mood.Wind  // every square follows these two
            s.ctrl = if (s.who == Who.Straight && !keepsMood) Pt((kp.x + s.to.x) / 2, (kp.y + s.to.y) / 2) else when (nextMood) {
                Mood.Burst -> Pt(.5f + (kp.x - .5f) * 3, .5f + (kp.y - .5f) * 3)
                Mood.Swirl -> { val a = atan2(kp.y - .5f, kp.x - .5f) + 2.2f; Pt(.5f + cos(a) * .55f, .5f + sin(a) * .55f) }
                Mood.Rain -> Pt(s.to.x + (rnd.nextFloat() - .5f) * .2f, -.45f)
                Mood.Snake -> Pt((kp.x + s.to.x) / 2 + (rnd.nextFloat() - .5f) * .5f, (kp.y + s.to.y) / 2 + (rnd.nextFloat() - .5f) * .5f)
                Mood.Matrix -> Pt(s.to.x, -.75f)                   // up out of sight, then straight down its column
                Mood.Wind -> Pt(1.45f + rnd.nextFloat() * .3f, (kp.y + s.to.y) / 2 + (rnd.nextFloat() - .5f) * .3f)
            }
            // Quicker since the owner's «переходы быстрее, сейчас пиксели долго "думают"» (≈ 13:01): delays about halved.
            val moodDelay = when (nextMood) {
                Mood.Burst -> rnd.nextFloat() * 120; Mood.Swirl -> rnd.nextFloat() * 150
                Mood.Rain -> rnd.nextFloat() * 300; Mood.Snake -> (i % 36) * 25f
                Mood.Matrix -> s.to.x * 700f + rnd.nextFloat() * 80   // column after column, left to right
                Mood.Wind -> (1 - s.to.x) * 450f + rnd.nextFloat() * 100
            }
            s.delay = if (explode) rnd.nextFloat() * 60 else                        // out of the blast straight into the flight
                (if (next.stagger > 0) (1 - s.to.y) * next.stagger else moodDelay) + if (s.who == Who.Shy) 150 + rnd.nextFloat() * 150 else 0f
            s.dur = (if (s.who == Who.Shy) 450f else 600f) + rnd.nextFloat() * 300
            s.spin = if (s.who == Who.Spinner) (if (rnd.nextBoolean()) -1 else 1) * PI_F * (1 + rnd.nextInt(2)) else 0f
        }
        fig = next; mood = nextMood; formedAt = t
        // the states with their own story stay a while longer (the net: about three waves of excitation)
        switchAt = t + 2500 + rnd.nextFloat() * 2500 + next.stagger + next.hold
    }

    /** Smart random of the next figure (the owner, ≈ 18:26: «нужен умный рандом переключения состояний живой анимации.
     *  Состояния, которые были показаны - теряют вес в случайности следующего показа» · «каждый раз, когда их не выбирают -
     *  вес растёт»): the chosen figure's weight drops to FIG_W_SHOWN, every figure not chosen gains FIG_W_GROW, up to FIG_W_MAX;
     *  the next one is drawn by weight. */
    private val figWeight = FloatArray(Fig.entries.size) { 1f }

    private fun pickFig(): Fig {
        val cands = Fig.entries.filter { it != fig && !(compact && it.fine) }
        var r = rnd.nextFloat() * cands.fold(0f) { s, f -> s + figWeight[f.ordinal] }
        var chosen = cands.last()
        for (c in cands) { r -= figWeight[c.ordinal]; if (r <= 0f) { chosen = c; break } }
        noteShown(chosen)
        return chosen
    }

    private fun noteShown(shown: Fig) {
        for (f in Fig.entries) figWeight[f.ordinal] = if (f == shown) FIG_W_SHOWN else min(FIG_W_MAX, figWeight[f.ordinal] + FIG_W_GROW)
    }

    fun start() { noteShown(Fig.Heart); regroup(0f, Fig.Heart, Mood.Burst) }

    /** Moves the spinner to time [t] (ms on the slowed clock): a due regroup, merges that just happened. */
    fun update(t: Float) {
        val tilesHold = tilesLevel(t) >= 10                       // the tiles square leaves only once fully built
        val quadHold = quadLevel(t) > 3.95f                       // the quadtree leaves only as single pixels again
        if (t > switchAt && (fig != Fig.Tiles || tilesHold) && (fig != Fig.Quad || quadHold)) {
            val moods = Mood.entries.filter { it != mood }
            regroup(t, pickFig(), moods[rnd.nextInt(moods.size)])
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
        state.compact = side0 < 40.dp.toPx()
        val ox = (size.width - side0) / 2; val oy = (size.height - side0) / 2
        val links = state.netLinks(now)
        if (links > 0f) {                                          // the faint links of the net and their sparks
            val stroke = side0 * .006f
            for ((a, b) in NET_EDGES) {
                val sa = state.sq[a]; val sb = state.sq[b]
                val pa = state.pos(sa, now); val la = state.lookOf(sa, a, now)
                val pb = state.pos(sb, now); val lb = state.lookOf(sb, b, now)
                val (glow, at) = state.netLink(a, b, now)
                val from = Offset(ox + (pa.x + la.dx) * side0, oy + (pa.y + la.dy) * side0)
                val to = Offset(ox + (pb.x + lb.dx) * side0, oy + (pb.y + lb.dy) * side0)
                // faintly pink even at rest (the owner, ≈ 18:08: «когда нет потенциалов бегущих по связям - почти не видно линий,
                // нужно чтобы чуточку их было видно розовыми»)
                drawLine(lerp(lerp(grey, pink, .45f), pink, glow).copy(alpha = links * (.32f + .5f * glow)), from, to, stroke)
                if (at >= 0f && glow > .05f) {
                    val sp = Offset(from.x + (to.x - from.x) * at, from.y + (to.y - from.y) * at)
                    drawCircle(pink.copy(alpha = links * glow), stroke * 2.2f, sp)
                }
            }
        }
        for (i in 0 until N) {
            val s = state.sq[i]; val k = state.progress(s, now); val home = k >= 1
            if (s.hidden || (s.host >= 0 && home)) continue        // waiting unseen, or merged into its host
            val p = state.pos(s, now); val l = state.lookOf(s, i, now)
            val gulp = if (s.gulpAt >= 0) max(0f, 1 - (now - s.gulpAt) / 380) else 0f
            val split = if (s.splitAt >= 0) max(0f, 1 - (now - s.splitAt - s.delay) / 300) else 0f
            val side = side0 * state.fig.size * l.k * (1 + .22f * sin(gulp * PI_F)) * (1 - .3f * split)
            val cx = ox + (p.x + l.dx) * side0
            val cy = oy + (p.y + l.dy) * side0
            translate(cx, cy) {
                rotateRad(s.spin * if (home) 1f else easeInOut(k), Offset.Zero) {
                    drawRect(l.c, Offset(-side / 2, -side / 2), Size(side, side))
                }
            }
        }
    }
}
