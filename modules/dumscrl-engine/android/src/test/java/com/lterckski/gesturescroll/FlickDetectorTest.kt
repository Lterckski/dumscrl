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

    /** Move the fingertip by (dxPalms, dyPalms) over ms at 30 fps; zero = hold still. */
    private fun move(ms: Long, dxPalms: Float = 0f, dyPalms: Float = 0f, pointing: Boolean = true) {
        val steps = (ms / 33).coerceAtLeast(1)
        repeat(steps.toInt()) {
            x += dxPalms * palm / steps; y += dyPalms * palm / steps
            t += 33
            d.feed(t, x, y, palm, pointing)?.let(out::add)
        }
    }

    @Test fun flickUpIsNextVideoDownIsPrevious() {
        move(300); move(150, dyPalms = -1.5f); move(1200); move(150, dyPalms = 1.5f); move(300)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_DOWN), out)
    }

    @Test fun flickToUsersLeftIsNextPhotoRightIsPrevious() { // frame not mirrored: user's left = larger x
        move(300); move(150, dxPalms = 1.5f); move(1200); move(150, dxPalms = -1.5f); move(300)
        assertEquals(listOf(Gesture.PHOTO_NEXT, Gesture.PHOTO_PREV), out)
    }

    @Test fun slowMoveIsNotAFlick() {
        move(300); move(1000, dyPalms = -1.5f); move(1000, dxPalms = 2f)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun onlyAPointingFingerFlicks() {
        move(300, pointing = false); move(150, dyPalms = -1.5f, pointing = false); move(300, pointing = false)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun cooldownIgnoresEveryFlick() {
        move(300); move(150, dyPalms = -1.5f); move(100); move(150, dyPalms = -1.5f); move(100); move(150, dxPalms = 1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun sameDirectionFiresAgainAfterCooldown() {
        move(300); move(150, dyPalms = -1.5f); move(400, dyPalms = 1.5f); move(700); move(150, dyPalms = -1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_UP), out)
    }

    @Test fun fingerComingBackAfterCooldownIsNotAReverseFlick() {
        // Flick up, keep moving until the cooldown ends, then snap back down without ever resting.
        move(300); move(150, dyPalms = -1.5f)
        repeat(8) { move(66, dxPalms = 0.3f); move(66, dxPalms = -0.3f) } // ~1 s of wiggling, never still
        move(150, dyPalms = 1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun reverseFlickCountsOnceTheFingerHasRested() {
        move(300); move(150, dyPalms = -1.5f); move(1100); move(150, dyPalms = 1.5f)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_DOWN), out)
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
}
