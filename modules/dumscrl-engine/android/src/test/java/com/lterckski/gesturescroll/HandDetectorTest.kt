package com.lterckski.gesturescroll

import org.junit.Assert.assertEquals
import org.junit.Test

class HandDetectorTest {
    private val d = HandDetector()
    private var t = 0L
    private var x = 320f
    private var y = 240f
    private val palm = 60f
    private val out = mutableListOf<Gesture>()

    /** Move the palm by (dxPalms, dyPalms) over ms at 30 fps; zero = hold still. */
    private fun move(ms: Long, dxPalms: Float = 0f, dyPalms: Float = 0f, open: Boolean = true, pose: Gesture? = null) {
        val steps = (ms / 33).coerceAtLeast(1)
        repeat(steps.toInt()) {
            x += dxPalms * palm / steps; y += dyPalms * palm / steps
            d.feed(t, x, y, palm, open, pose)?.let(out::add); t += 33
        }
    }

    @Test fun swipeUpThenHandComesBackIsOneScroll() {
        move(300); move(250, dyPalms = -3f); move(400, dyPalms = 3f); move(500)
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun swipeDown() {
        move(300); move(250, dyPalms = 3f); move(500)
        assertEquals(listOf(Gesture.SWIPE_DOWN), out)
    }

    @Test fun swipeToUsersLeftAndRight() {
        move(300); move(250, dxPalms = 3f); move(900); move(250, dxPalms = -3f); move(500)
        assertEquals(listOf(Gesture.SWIPE_LEFT, Gesture.SWIPE_RIGHT), out)
    }

    @Test fun repeatedSwipesSameWayEachScroll() {
        move(300)
        repeat(3) { move(250, dyPalms = -3f); move(700, dyPalms = 3f, open = false) } // hand closed on the way back
        assertEquals(List(3) { Gesture.SWIPE_UP }, out)
    }

    @Test fun closedHandOrSlowDriftDoesNothing() {
        move(600, open = false); move(250, dyPalms = -3f, open = false); move(250, dxPalms = 3f, open = false)
        move(500, open = true); move(800, dyPalms = 0.8f); move(800, dxPalms = -0.8f)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun flickWithHandBlurredOutMidwayStillSwipes() {
        move(300); move(100, dyPalms = -0.5f); t += 120; move(100, dyPalms = -0.6f); move(500) // 120 ms with no hand
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun handReappearingElsewhereIsNotASwipe() {
        move(300); t += 1000; y -= 3 * palm; move(500)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun openPalmHeldTogglesOnce() {
        move(2500)
        assertEquals(listOf(Gesture.TOGGLE), out)
    }

    @Test fun pointThenPeaceEachFireOnce() {
        move(300); move(800, pose = Gesture.SWIPE_UP, open = false); move(800, pose = Gesture.SWIPE_DOWN, open = false)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_DOWN), out)
    }

    @Test fun signSurvivesOneMisreadFrameAndDoesNotRepeat() {
        move(300)
        repeat(30) { move(33, pose = if (it == 5) null else Gesture.SWIPE_UP, open = false) }
        assertEquals(listOf(Gesture.SWIPE_UP), out)
    }

    @Test fun sameSignFiresAgainAfterHandLeaves() {
        move(300); move(600, pose = Gesture.SWIPE_UP, open = false); t += 1000; move(600, pose = Gesture.SWIPE_UP, open = false)
        assertEquals(listOf(Gesture.SWIPE_UP, Gesture.SWIPE_UP), out)
    }

    @Test fun signModeIgnoresFlicks() {
        val signs = HandDetector(swipes = false)
        var tt = 0L; var yy = 300f
        val got = mutableListOf<Gesture>()
        repeat(30) { signs.feed(tt, 300f, yy, 60f, true, null)?.let(got::add); tt += 33; if (it > 10) yy -= 20f }
        assertEquals(emptyList<Gesture>(), got)
    }

    @Test fun thumbsUpLikesOnce() {
        move(300); move(1000, pose = Gesture.THUMBS_UP, open = false); move(300, open = false)
        assertEquals(listOf(Gesture.THUMBS_UP), out)
    }
}
