package com.kriniks.kartun

import com.kriniks.kartun.UpscaleViewModel.Companion.fits
import com.kriniks.kartun.UpscaleViewModel.Companion.passFactor
import com.kriniks.kartun.UpscaleViewModel.Companion.secondPassInput
import com.kriniks.kartun.UpscaleViewModel.Companion.tiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM hygiene for the pass plan of plans/04 step 1: which scales take a second pass and over what image, how much work
 * the estimate counts, the tile count the progress ring divides by, and the 500 MP output limit. Hygiene only: whether
 * the two passes produce a right picture is read on the device.
 */
class PassPlanTest {

    @Test fun oneTwoAndFourAreOnePass() {
        assertNull(secondPassInput(1)); assertNull(secondPassInput(2)); assertNull(secondPassInput(4))
    }

    // 8×: the 4× result scaled down to 2× feeds the second pass; 16×: the 4× result itself.
    @Test fun eightAndSixteenTakeASecondPass() {
        assertEquals(2, secondPassInput(8)); assertEquals(4, secondPassInput(16))
    }

    // The estimate counts the megapixels fed to the network: the photo, plus 2² or 4² of it for the second pass.
    @Test fun passFactorCountsBothPasses() {
        assertEquals(1f, passFactor(2), 0f); assertEquals(5f, passFactor(8), 0f); assertEquals(17f, passFactor(16), 0f)
    }

    // The same count the native loop reports: the 960x1280 photo is 5 x 7 tiles of 200 px (35, as the Titan showed).
    @Test fun tilesMatchTheEngine() {
        assertEquals(35, tiles(960, 1280)); assertEquals(1, tiles(200, 200)); assertEquals(4, tiles(201, 201))
    }

    // The 536 MP output limit (the owner, chat 2026-10-16 ≈ 15:13 +03:00: «лимит Android ~2,1 ГБ/536 М - тогда вот такой
    // лимит делаем»): the owner's 960x1280 photo fits even at 16× (314.6 MP); the boundary itself is 33.5 MP at 4× = 536 MP.
    @Test fun outputLimitOnTheOwnersPhoto() {
        val mp = 960f * 1280 / 1_000_000f
        assertTrue(fits(mp, 8)); assertTrue(fits(mp, 16))
        assertTrue(fits(33.5f, 4)); assertFalse(fits(33.6f, 4))
    }

    // The limit stays inside Android's ceiling for one Bitmap: width x height x 4 bytes <= 2^31 - 1.
    @Test fun outputLimitFitsOneBitmap() {
        assertTrue(UpscaleViewModel.MAX_OUTPUT_MP * 1_000_000.0 * 4 <= Int.MAX_VALUE.toDouble())
    }

    // A 100 MP photo: 2× gives 400 MP and fits, 4× gives 1600 MP and does not.
    @Test fun outputLimitOnA100MpPhoto() {
        assertTrue(fits(100f, 2)); assertFalse(fits(100f, 4))
    }

    // The largest input since 2026-10-16 (200 MP, the owner: «давай лимит входных поднимем до 200 мегапикселей»): 1× fits,
    // 2× gives 800 MP and does not.
    @Test fun outputLimitOnTheLargestInput() {
        assertEquals(200f, UpscaleViewModel.MAX_INPUT_MP, 0f)
        assertTrue(fits(200f, 1)); assertFalse(fits(200f, 2))
    }
}
