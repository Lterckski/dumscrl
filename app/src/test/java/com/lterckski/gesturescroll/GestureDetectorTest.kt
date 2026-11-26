package com.lterckski.gesturescroll

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureDetectorTest {
    private val d = GestureDetector()
    private var t = 0L
    private val out = mutableListOf<Gesture>()

    /** Hold a pose for ms, sampling at 30 fps. */
    private fun hold(ms: Long, pitch: Float = 0.5f, roll: Float = 0f) {
        val end = t + ms
        while (t < end) { d.feed(t, pitch, roll)?.let(out::add); t += 33 }
    }

    @Test fun neutralAndSmallJitterDoNothing() {
        hold(2000)
        repeat(30) { hold(100, 0.5f + if (it % 2 == 0) 0.03f else -0.03f, if (it % 2 == 0) 3f else -3f) }
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun singleNod() {
        hold(1000); hold(250, pitch = 0.75f); hold(1500)
        assertEquals(listOf(Gesture.NOD), out)
    }

    @Test fun doubleNod() {
        hold(1000); hold(250, pitch = 0.75f); hold(200); hold(250, pitch = 0.75f); hold(1500)
        assertEquals(listOf(Gesture.DOUBLE_NOD), out)
    }

    @Test fun lookingDownLongIsNotANod() {
        hold(1000); hold(2000, pitch = 0.75f); hold(1500)
        assertEquals(emptyList<Gesture>(), out)
    }

    @Test fun quickTiltsBothWays() {
        hold(1000); hold(400, roll = 25f); hold(1000); hold(400, roll = -25f); hold(1000)
        assertEquals(listOf(Gesture.TILT_LEFT, Gesture.TILT_RIGHT), out)
    }

    @Test fun holdTiltTogglesOnceNotTilt() {
        hold(1000); hold(1500, roll = 25f); hold(1000)
        assertEquals(listOf(Gesture.TOGGLE), out)
        hold(1500, roll = -25f); hold(1000)
        assertEquals(listOf(Gesture.TOGGLE, Gesture.TOGGLE), out)
    }
}
