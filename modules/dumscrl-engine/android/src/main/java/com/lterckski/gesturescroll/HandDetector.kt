package com.lterckski.gesturescroll

import kotlin.math.hypot

/**
 * Turns a stream of hand samples into held signs and the open-palm pause hold.
 * (Moving gestures are FlickDetector's.)
 *
 * x, y: palm centre in image pixels. palm: wrist-to-middle-knuckle length in pixels; distances are measured
 * in palms, so it works the same near or far from the camera. open: at least three fingers extended.
 */
class HandDetector(
    private val gapMs: Long = 300,
    private val cooldownMs: Long = 500,
    private val holdMs: Long = 2000, // long, so a resting hand rarely pauses by accident
    private val stillPalms: Float = 0.3f,
    private val poseMs: Long = 300,
) {
    private var lastSeenAt = -1L
    private var cooldownUntil = 0L
    private var holdStart = -1L
    private var holdX = 0f
    private var holdY = 0f
    private var holdConsumed = false
    private val poses = ArrayDeque<Pair<Long, Gesture?>>()
    private var firedPose: Gesture? = null

    /** Call when no hand is visible. */
    fun lost() {
        holdStart = -1; holdConsumed = false
    }

    /** pose: a held hand sign (thumbs up, pointing, peace…) and the gesture it stands for, or null. */
    fun feed(t: Long, x: Float, y: Float, palm: Float, open: Boolean, pose: Gesture?): Gesture? {
        if (t - lastSeenAt > gapMs) { poses.clear(); firedPose = null } // a new appearance, not the same sign
        lastSeenAt = t

        signFired(t, pose)?.let { return it }
        if (pose != null) { holdStart = -1; return null }

        // Open palm held still = pause/resume. Fires once per hold.
        if (!open) {
            holdStart = -1; holdConsumed = false
        } else if (holdStart < 0 || hypot(x - holdX, y - holdY) / palm > stillPalms) {
            holdStart = t; holdX = x; holdY = y
        } else if (!holdConsumed && t - holdStart >= holdMs) {
            holdConsumed = true
            return emit(t, Gesture.TOGGLE)
        }
        return null
    }

    /**
     * A sign fires when it has been the clear majority (70%) over the last ~0.3 s, so one misread
     * frame doesn't break it. It can't fire again until the sign changes or the hand leaves.
     */
    private fun signFired(t: Long, pose: Gesture?): Gesture? {
        poses.addLast(t to pose)
        while (t - poses.first().first > poseMs) poses.removeFirst()
        if (t - poses.first().first < poseMs * 0.8) return null // not watched long enough yet
        val (top, count) = poses.groupingBy { it.second }.eachCount().maxBy { it.value }.toPair()
        if (count < poses.size * 0.7f || top == firedPose || t < cooldownUntil) return null
        firedPose = top
        return top?.let { emit(t, it) }
    }

    private fun emit(t: Long, g: Gesture): Gesture {
        cooldownUntil = t + cooldownMs
        return g
    }
}
