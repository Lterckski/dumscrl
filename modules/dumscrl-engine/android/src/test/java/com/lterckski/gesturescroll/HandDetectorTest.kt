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

    @Test fun movingOpenHandDoesNothing() { // moving gestures are FlickDetector's
        move(300); move(250, dyPalms = -3f); move(250, dxPalms = 3f); move(500)
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

    @Test fun thumbsUpLikesOnce() {
        move(300); move(1000, pose = Gesture.THUMBS_UP, open = false); move(300, open = false)
        assertEquals(listOf(Gesture.THUMBS_UP), out)
    }

    @Test fun signSeenWhileTheWristTurnsDoesNotFireUntilHeldStill() {
        // The thumb tip sweeps round as the wrist turns (as a relaxed thumb passing "thumb left" does).
        fun turning(ms: Long, degPerFrame: Double) {
            var a = 0.0
            repeat((ms / 33).toInt()) {
                a += Math.toRadians(degPerFrame)
                val tip = floatArrayOf(x, y, x + palm * Math.cos(a).toFloat(), y + palm * Math.sin(a).toFloat())
                d.feed(t, x, y, palm, false, Gesture.PHOTO_NEXT, tip)?.let(out::add); t += 33
            }
        }
        move(300)
        turning(900, 6.0) // ~6° a frame: never still for 0.3 s
        assertEquals(emptyList<Gesture>(), out)
        turning(600, 0.0) // stops and holds the sign
        assertEquals(listOf(Gesture.PHOTO_NEXT), out)
    }
}
