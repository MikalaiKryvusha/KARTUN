package com.kriniks.kartun

import com.kriniks.kartun.UpscaleViewModel.Companion.MAX_OUTPUT_MP
import com.kriniks.kartun.UpscaleViewModel.Companion.limitedFactor
import com.kriniks.kartun.UpscaleViewModel.Companion.limitedSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fractional scale at the output limit (plans/08 step 9; the owner 2026-10-16 ≈ 16:10: «да, правильно» to «4× для
 * этого фото не поместится, можем 3,2×»): the result is the largest size of the photo's proportions within the limit,
 * and the slider shows its factor rounded down to one decimal.
 */
class LimitedSizeTest {

    /** The 50 MP test photo of the Titan (chimp50mp.jpg, 8640 × 5760) at 4×. */
    @Test fun fiftyMegapixelsAtFourIsLimited() {
        val (w, h) = limitedSize(8640, 5760, 4)!!
        assertTrue("within the limit: $w x $h", w.toLong() * h <= (MAX_OUTPUT_MP * 1_000_000).toLong())
        assertTrue("near the limit: $w x $h", w.toLong() * h > (MAX_OUTPUT_MP * 0.999 * 1_000_000).toLong())
        assertEquals(3.2f, limitedFactor(8640, w), 0.0001f)
        // Proportions kept to a pixel.
        assertEquals(8640.0 / 5760, w.toDouble() / h, 0.0002)
    }

    @Test fun aScaleThatFitsIsNotLimited() = assertNull(limitedSize(4000, 3000, 4))

    /** 8× of a 12 MP photo (96 × 12 = 768 MP) is limited between 4× and 8×. */
    @Test fun eightOfTwelveMegapixels() {
        val (w, _) = limitedSize(4000, 3000, 8)!!
        assertEquals(6.6f, limitedFactor(4000, w), 0.0001f)
    }
}
