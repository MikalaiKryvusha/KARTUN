package com.kriniks.kartun

import com.kriniks.kartun.ui.NEVER_SHOWN
import com.kriniks.kartun.ui.smartPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The live spinner's smart random never brings a figure back soon (the owner, 2026-10-16 ≈ 15:50: «состояния, которые вот
 *  только что были, опять показываются через два состояния»). */
class SmartPickTest {
    /** Runs [n] picks over a pool of [size] figures from a start figure, the way the spinner does; returns the sequence. */
    private fun run(size: Int, n: Int, seed: Int): List<Int> {
        val pool = (0 until size).toList()
        val last = IntArray(size) { NEVER_SHOWN }
        val rnd = Random(seed)
        var picks = 0
        last[0] = picks++ // the spinner starts on the heart
        val seq = mutableListOf(0)
        repeat(n) {
            val f = smartPick(pool, last, picks, rnd)
            last[f] = picks++
            seq += f
        }
        return seq
    }

    private fun checkNoEarlyRepeat(size: Int) {
        for (seed in 1..5) {
            val seq = run(size, 10_000, seed)
            val gap = size / 2 // a figure must wait at least half the pool before it comes again
            for (i in seq.indices) for (j in 1..gap) if (i + j < seq.size)
                assertTrue("pool $size, seed $seed: figure ${seq[i]} again after $j picks at $i", seq[i] != seq[i + j])
            assertEquals("every figure is shown", size, seq.toSet().size)
        }
    }

    @Test fun nineFiguresNeverRepeatWithinFour() = checkNoEarlyRepeat(9)

    @Test fun sixCompactFiguresNeverRepeatWithinThree() = checkNoEarlyRepeat(6)

    @Test fun theLongestUnseenComesMostOften() {
        // After a full round the figure unseen the longest (age 9) outweighs one shown 5 picks ago (age 5): 81 to 25.
        val last = IntArray(9) { it } // figure i shown at pick i
        var oldest = 0
        val rnd = Random(7)
        repeat(1_000) { if (smartPick((0 until 9).toList(), last, 9, rnd) == 0) oldest++ }
        assertTrue("oldest drawn $oldest of 1000", oldest > 300)
    }
}
