package com.kriniks.kartun

import androidx.compose.ui.geometry.Offset
import com.kriniks.kartun.ui.pinchStep
import org.junit.Assert.assertEquals
import org.junit.Test

/** The point between the two fingers stays under them while they zoom (the owner, chat 2026-10-16 ≈ 15:31: «словно центр
 *  тяжести оказывается не между этими двумя пальцами, а в центре фото»). */
class PinchTest {
    private val centre = Offset(800f, 600f)

    /** Where a picture point [p] (in unzoomed screen coordinates) is drawn at [zoom] and [pan], scaled about [centre]. */
    private fun drawn(p: Offset, zoom: Float, pan: Offset) = centre + pan + (p - centre) * zoom

    @Test fun pointUnderTheFingersStaysPut() {
        var zoom = 1.5f
        var pan = Offset(-120f, 40f)
        val fingers = Offset(300f, 950f)
        // The picture point under the fingers before the step.
        val p = centre + (fingers - centre - pan) / zoom
        val (z, np) = pinchStep(zoom, pan, fingers, centre, 1.6f, Offset.Zero, 8f)
        zoom = z; pan = np
        val s = drawn(p, zoom, pan)
        assertEquals(fingers.x, s.x, 0.01f)
        assertEquals(fingers.y, s.y, 0.01f)
    }

    @Test fun thePointFollowsTheFingersPan() {
        val fingers = Offset(1200f, 200f)
        val move = Offset(30f, -50f)
        val p = centre + (fingers - centre) / 1f
        val (z, pan) = pinchStep(1f, Offset.Zero, fingers, centre, 2f, move, 8f)
        val s = drawn(p, z, pan)
        assertEquals(fingers.x + move.x, s.x, 0.01f)
        assertEquals(fingers.y + move.y, s.y, 0.01f)
    }

    @Test fun zoomStaysWithinLimits() {
        assertEquals(1f, pinchStep(1.2f, Offset.Zero, centre, centre, 0.5f, Offset.Zero, 8f).first, 0f)
        assertEquals(8f, pinchStep(6f, Offset.Zero, centre, centre, 2f, Offset.Zero, 8f).first, 0f)
    }
}
