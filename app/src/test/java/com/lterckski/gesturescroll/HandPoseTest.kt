package com.lterckski.gesturescroll

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class HandPoseTest {
    /** A hand pointing up (y down, wrist at 0,0, palm length 100). */
    private fun hand(index: Boolean, middle: Boolean, ring: Boolean, pinky: Boolean, thumb: String = "tucked", turnDeg: Double = 0.0, pinkySide: Boolean = false): HandShape {
        val p = Array(21) { 0f to 0f }
        p[0] = 0f to 0f
        // thumb: CMC 1, MCP 2, IP 3, tip 4
        p[1] = -30f to -20f; p[2] = -50f to -50f
        when (thumb) {
            "out" -> { p[3] = -90f to -70f; p[4] = -130f to -80f }
            "up" -> { p[3] = -50f to -120f; p[4] = -50f to -200f }
            "right" -> { p[3] = -100f to -55f; p[4] = -170f to -60f }
            "left" -> { p[3] = 10f to -55f; p[4] = 70f to -60f }
            else -> { p[3] = -20f to -80f; p[4] = 10f to -90f }
        }
        listOf(index, middle, ring, pinky).forEachIndexed { i, ext ->
            val x = -30f + 20f * i
            val base = 5 + 4 * i // MCP, PIP, DIP, tip
            p[base] = x to -100f
            p[base + 1] = x to -140f
            if (ext) { p[base + 2] = x to -170f; p[base + 3] = x to -200f }
            else { p[base + 2] = x to -120f; p[base + 3] = x to -105f }
        }
        if (pinkySide) { p[17] = 30f to -100f; p[18] = 70f to -100f; p[19] = 100f to -100f; p[20] = 130f to -100f }
        val r = Math.toRadians(turnDeg)
        val px = FloatArray(21) { (p[it].first * cos(r) - p[it].second * sin(r)).toFloat() }
        val py = FloatArray(21) { (p[it].first * sin(r) + p[it].second * cos(r)).toFloat() }
        return classifyHand(px, py)
    }

    @Test fun signs() {
        assertEquals(Gesture.SWIPE_UP, hand(true, false, false, false).pose)
        assertEquals(Gesture.SWIPE_DOWN, hand(true, true, false, false).pose)
        assertEquals(Gesture.SWIPE_LEFT, hand(true, true, true, false).pose)
        assertEquals(Gesture.SWIPE_RIGHT, hand(true, true, true, true).pose)
        assertEquals(Gesture.THUMBS_UP, hand(false, false, false, false, thumb = "up").pose)
    }

    @Test fun thumbLeftAndSidewaysPinkyFlipPhotos() {
        // image right = the user's left (camera frames aren't mirrored)
        assertEquals(Gesture.PHOTO_NEXT, hand(false, false, false, false, thumb = "left").pose)
        assertEquals(null, hand(false, false, false, false, thumb = "right").pose)
        assertEquals(Gesture.PHOTO_PREV, hand(false, false, false, true, pinkySide = true).pose)
        assertEquals(null, hand(false, false, false, true).pose) // pinky straight up isn't a sign
        assertEquals(Gesture.THUMBS_UP, hand(false, false, false, false, thumb = "up").pose)
        assertEquals(Gesture.SWIPE_UP, hand(true, false, false, false, turnDeg = -90.0).pose) // index sideways stays "point"
    }

    @Test fun openPalmAndFistAreNotSigns() {
        val palm = hand(true, true, true, true, thumb = "out")
        assertEquals(null, palm.pose); assertEquals(true, palm.open); assertEquals("open palm", palm.name)
        assertEquals(null, hand(false, false, false, false).pose)
    }

    @Test fun fingerCountsSurviveATurnedHand() {
        assertEquals(Gesture.SWIPE_DOWN, hand(true, true, false, false, turnDeg = 40.0).pose)
        assertEquals(Gesture.SWIPE_LEFT, hand(true, true, true, false, turnDeg = -50.0).pose)
    }
}
