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
 * the estimate counts, the tile count the progress ring divides by, and the 200 MP output limit. Hygiene only: whether
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

    // The owner's photo 960x1280 (1.2288 MP): 8× gives 78.6 MP and fits, 16× gives 314.6 MP and does not.
    @Test fun outputLimitOnTheOwnersPhoto() {
        val mp = 960f * 1280 / 1_000_000f
        assertTrue(fits(mp, 8)); assertFalse(fits(mp, 16))
        assertTrue(fits(12.5f, 4)); assertFalse(fits(12.6f, 4))
    }
}
