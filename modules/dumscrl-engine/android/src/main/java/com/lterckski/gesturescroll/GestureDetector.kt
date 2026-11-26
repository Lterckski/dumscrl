package com.lterckski.gesturescroll

import kotlin.math.abs

enum class Gesture {
    NOD, DOUBLE_NOD, TILT_LEFT, TILT_RIGHT, // head
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, THUMBS_UP, PHOTO_NEXT, PHOTO_PREV, // hand
    TOGGLE, // pause/resume, from either
}

/**
 * Turns a stream of head-pose samples into gestures.
 *
 * pitch: how far the nose sits below the eye line, in eye-widths (grows when the head nods down).
 * roll: head tilt in degrees, positive = tilted to the user's left.
 * Both come from eyes/nose/forehead only, never the jaw, so chewing and talking barely move them.
 * Each signal is compared to a slowly drifting baseline, so a habitual head angle is "neutral".
 *
 * Thresholds are first guesses; calibrate on the real phone.
 */
class GestureDetector(
    private val nodThreshold: Float = 0.10f,
    private val tiltThresholdDeg: Float = 15f,
    private val nodMaxMs: Long = 700,
    private val doubleNodWindowMs: Long = 800,
    private val tiltMaxMs: Long = 1000,
    private val toggleHoldMs: Long = 1000,
    private val cooldownMs: Long = 600,
) {
    private var pitchBase = Float.NaN
    private var rollBase = Float.NaN
    private var pitchSmooth = Float.NaN
    private var rollSmooth = Float.NaN

    private var nodStart = -1L
    private var pendingNodAt = -1L
    private var tiltStart = -1L
    private var tiltDir = 0
    private var tiltConsumed = false
    private var cooldownUntil = 0L

    /** Call when no face is visible: drop any half-finished gesture. */
    fun reset() {
        nodStart = -1; pendingNodAt = -1; tiltStart = -1; tiltConsumed = false
        pitchSmooth = Float.NaN; rollSmooth = Float.NaN
    }

    /** Feed one sample; returns a gesture when one completes. */
    fun feed(tMs: Long, pitch: Float, roll: Float): Gesture? {
        // Light smoothing kills landmark jitter without hiding a real nod.
        pitchSmooth = if (pitchSmooth.isNaN()) pitch else pitchSmooth * 0.5f + pitch * 0.5f
        rollSmooth = if (rollSmooth.isNaN()) roll else rollSmooth * 0.5f + roll * 0.5f
        if (pitchBase.isNaN()) { pitchBase = pitchSmooth; rollBase = rollSmooth }
        val dp = pitchSmooth - pitchBase
        val dr = rollSmooth - rollBase

        val result = tilt(tMs, dr) ?: if (tiltStart < 0) nod(tMs, dp) else null

        // Baseline follows only while the head is near neutral, so a gesture never becomes the new neutral.
        if (tiltStart < 0 && nodStart < 0 && abs(dp) < nodThreshold / 2 && abs(dr) < tiltThresholdDeg / 2) {
            pitchBase += (pitchSmooth - pitchBase) * 0.02f
            rollBase += (rollSmooth - rollBase) * 0.02f
        }
        return result?.let { emit(tMs, it) }
    }

    private fun tilt(t: Long, dr: Float): Gesture? {
        if (tiltStart < 0) {
            if (abs(dr) > tiltThresholdDeg && t >= cooldownUntil) {
                tiltStart = t; tiltDir = if (dr > 0) 1 else -1; tiltConsumed = false
                nodStart = -1 // a tilt cancels a half-done nod
            }
            return null
        }
        val held = t - tiltStart
        if (abs(dr) < tiltThresholdDeg / 2) { // back to neutral
            tiltStart = -1
            return if (!tiltConsumed && held <= tiltMaxMs) {
                if (tiltDir > 0) Gesture.TILT_LEFT else Gesture.TILT_RIGHT
            } else null
        }
        if (!tiltConsumed && held >= toggleHoldMs && abs(dr) > tiltThresholdDeg) {
            tiltConsumed = true
            return Gesture.TOGGLE
        }
        return null
    }

    private fun nod(t: Long, dp: Float): Gesture? {
        if (pendingNodAt >= 0 && t - pendingNodAt > doubleNodWindowMs) {
            pendingNodAt = -1
            return Gesture.NOD
        }
        if (nodStart < 0) {
            if (dp > nodThreshold && t >= cooldownUntil) nodStart = t
            return null
        }
        if (dp < nodThreshold / 2) { // head came back up
            val quick = t - nodStart <= nodMaxMs
            nodStart = -1
            if (!quick) return null // held down = looking at something, not a nod
            if (pendingNodAt >= 0) { pendingNodAt = -1; return Gesture.DOUBLE_NOD }
            pendingNodAt = t
        }
        return null
    }

    private fun emit(t: Long, g: Gesture): Gesture? {
        if (g == Gesture.TOGGLE) pendingNodAt = -1
        // Second nod of a double is part of the same gesture, so it doesn't start a cooldown before it.
        cooldownUntil = t + cooldownMs
        return g
    }
}
