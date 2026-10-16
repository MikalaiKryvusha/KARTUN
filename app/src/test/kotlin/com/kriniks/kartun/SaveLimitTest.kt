package com.kriniks.kartun

import com.kriniks.kartun.ResultStore.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The format limits of the save dialog (the owner, chat 2026-10-16 ≈ 15:14 +03:00: «предупреждать в диалоге сохранения,
 *  и предложить выполнить сжатие до допустимого размера»): WebP 16 383 px, JPEG 65 500 px per side, PNG none. */
class SaveLimitTest {
    @Test fun fittingResultIsUnchanged() {
        assertEquals(16_383 to 9_000, ResultStore.fitSize(Format.Webp, 16_383, 9_000))
        assertEquals(50_928 to 26_476, ResultStore.fitSize(Format.Png, 50_928, 26_476))
        assertTrue(ResultStore.fits(Format.Jpeg, 50_928, 26_476))
    }

    @Test fun webpIsReducedToItsSideKeepingTheAspect() {
        // The Zurich test photo at 4×: 50 928 x 26 476 -> the longer side 16 383, the shorter one in proportion.
        val (w, h) = ResultStore.fitSize(Format.Webp, 50_928, 26_476)
        assertEquals(16_383, w)
        assertEquals(Math.round(26_476 * 16_383.0 / 50_928).toInt(), h)
        assertFalse(ResultStore.fits(Format.Webp, 50_928, 26_476))
    }

    @Test fun tallPictureIsReducedByItsHeight() {
        assertEquals(10_000 to 65_500, ResultStore.fitSize(Format.Jpeg, 20_000, 131_000))
    }
}
