package com.kriniks.kartun.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM hygiene for the number formats the screens show (plans/03): sizes as the mockups write them, and the minutes /
 * seconds split behind «≈ 7 секунд» / «≈ 1 минута 49 секунд» (the owner, 2026-10-14: «минут секунд … целым слвом»).
 * Hygiene, not a test of the product: the words themselves come from `plurals` and are read on the device.
 */
class FormatTest {

    @Test fun sizeBelowTenThousandHasNoSeparator() = assertEquals("3840 × 5120", sizeText(3840, 5120))

    // The mockups write «7 680 × 10 240»: a thin space from 10 000 on, and 7680 alone stays without one.
    @Test fun sizeFromTenThousandGetsThinSpace() = assertEquals("7680 × 10 240", sizeText(7680, 10_240))

    @Test fun sizeBoundaryIsExactlyTenThousand() {
        assertEquals("9999 × 10 000", sizeText(9_999, 10_000))
    }

    @Test fun durationRoundsToNearestSecond() {
        assertEquals(0 to 7, durationParts(6_600))   // the Titan's 6.6 s photo run -> «7 секунд»
        assertEquals(0 to 6, durationParts(6_499))
    }

    @Test fun durationNeverShowsZero() = assertEquals(0 to 1, durationParts(0))

    @Test fun durationSplitsMinutes() {
        assertEquals(1 to 49, durationParts(108_700)) // the x4plus estimate seen on the Titan: «1 минута 49 секунд»
        assertEquals(2 to 0, durationParts(120_000))  // whole minutes -> the seconds part is dropped by durationText
    }
}
