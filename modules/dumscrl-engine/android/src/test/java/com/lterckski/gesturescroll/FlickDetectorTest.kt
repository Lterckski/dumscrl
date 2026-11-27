package com.lterckski.gesturescroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlickDetectorTest {
    private val d = FlickDetector(cooldownMs = 1000)
    private var t = 0L
    private var x = 320f
    private var y = 240f
    private val palm = 60f
    private val out = mutableListOf<Gesture>()

    /** Move the fingertip by (dxPalms, dyPalms) over ms at 30 fps; zero = hold still. fingers: 1 index, 2 index + middle. */
    private fun move(ms: Long, dxPalms: Float = 0f, dyPalms: Float = 0f, fingers: Int? = 1) {
        val steps = (ms / 33).coerceAtLeast(1)
        repeat(steps.toInt()) {
            x += dxPalms * palm / steps; y += dyPalms * palm / steps
            t += 33
            d.feed(t, x, y, palm, fingers)?.let(out::add)
        }
    }

    @Test fun indexFlickEitherWayIsNextVideo() {
        move(300); move(150, dyPalms = -1.5f); move(1200); move(150, dyPalms = 1.5f); move(300)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_UP), out)
    }

    @Test fun indexAndMiddleFlickEitherWayIsPreviousVideo() {
        move(300, fingers = 2); move(150, dyPalms = 1.5f, fingers = 2); move(1200, fingers = 2); move(150, dyPalms = -1.5f, fingers = 2)
        assertEquals(listOf(Gesture.SWIPE_DOWN, Gesture.SWIPE_DOWN), out)
    }

    @Test fun oneBlurredFrameDoesNotChangeWhichFingers() {
        move(300, fingers = 2)
        repeat(5) { move(33, dyPalms = -0.3f, fingers = if (it == 2) 1 else 2) }
        move(100, fingers = 2)
        assertEquals(listOf(Gesture.SWIPE_DOWN), out)
    }

    @Test fun indexSidewaysFlipsPhotos() { // frame not mirrored: user's left = larger x
        move(300); move(150, dxPalms = 1.5f); move(1200); move(150, dxPalms = -1.5f); move(300)
        assertEquals(listOf(Gesture.PHOTO_NEXT, Gesture.PHOTO_PREV), out)
    }

    @Test fun twoFingersSidewaysDoNothing() {
        move(300, fingers = 2); move(150, dxPalms = 1.5f, fingers = 2); move(300, fingers = 2)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun slowMoveIsNotAFlick() {
        move(300); move(1000, dyPalms = -1.5f); move(1000, dxPalms = 2f)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun otherHandShapesDoNotFlick() {
        move(300, fingers = null); move(150, dyPalms = -1.5f, fingers = null); move(300, fingers = null)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun cooldownIgnoresEveryFlick() {
        move(300); move(150, dyPalms = -1.5f); move(100); move(150, dyPalms = -1.5f); move(100); move(150, dxPalms = 1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun sameWayFiresAgainAfterCooldown() {
        move(300); move(150, dyPalms = -1.5f); move(400, dyPalms = 1.5f); move(700); move(150, dyPalms = -1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_UP), out)
    }

    @Test fun fingerComingBackWithoutRestingIsNotASecondFlick() {
        // Flick up, keep moving until the wait ends, then snap back down without ever resting.
        move(300); move(150, dyPalms = -1.5f)
        repeat(8) { move(66, dxPalms = 0.3f); move(66, dxPalms = -0.3f) } // ~1 s of wiggling, never still
        move(150, dyPalms = 1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun jitterCountsAsResting() {
        move(300); move(150, dyPalms = -1.5f)
        repeat(30) { move(33, dyPalms = if (it % 2 == 0) 0.03f else -0.03f) } // ~1 palm/s frame to frame, still in place
        move(150, dyPalms = 1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_UP), out)
    }

    @Test fun cooldownIsAdjustableLive() {
        d.cooldownMs = 300
        move(300); move(150, dyPalms = -1.5f); move(400); move(150, dyPalms = -1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_UP), out)
    }

    @Test fun speedReadout() {
        move(300); assertTrue(d.speed < 1f)
        move(132, dyPalms = -2f); assertTrue(d.speed > d.minSpeed)
    }

    @Test fun flickStillCountsWhenTheFingersBlurIntoAFistMidway() {
        move(300, fingers = 2); move(150, dyPalms = 1.5f, fingers = null); move(300, fingers = 2)
        assertEquals(listOf(Gesture.SWIPE_DOWN), out)
    }

    @Test fun movingFistLongAfterPointingIsNotAFlick() {
        move(300); move(600, fingers = null); move(150, dyPalms = -1.5f, fingers = null)
        assertEquals(emptyList<Gesture>(), out)
    }
}
