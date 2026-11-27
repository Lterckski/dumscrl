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
    private val stillSignPalms: Float = 0.35f,
) {
    private var lastSeenAt = -1L
    private var cooldownUntil = 0L
    private var holdStart = -1L
    private var holdX = 0f
    private var holdY = 0f
    private var holdConsumed = false
    private class Seen(val t: Long, val pose: Gesture?, val points: FloatArray, val palm: Float)
    private val poses = ArrayDeque<Seen>()
    private var firedPose: Gesture? = null

    /** Call when no hand is visible. */
    fun lost() {
        holdStart = -1; holdConsumed = false
    }

    /**
     * pose: a held hand sign (thumbs up, pointing, peace…) and the gesture it stands for, or null.
     * points: key landmarks (x0,y0,x1,y1… pixels). A sign only fires while they all stay put, so a sign
     * the hand passes through while the wrist turns doesn't count.
     */
    fun feed(t: Long, x: Float, y: Float, palm: Float, open: Boolean, pose: Gesture?, points: FloatArray = floatArrayOf(x, y)): Gesture? {
        if (t - lastSeenAt > gapMs) { poses.clear(); firedPose = null } // a new appearance, not the same sign
        lastSeenAt = t

        signFired(t, pose, points, palm)?.let { return it }
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
    private fun signFired(t: Long, pose: Gesture?, points: FloatArray, palm: Float): Gesture? {
        poses.addLast(Seen(t, pose, points, palm))
        while (t - poses.first().t > poseMs) poses.removeFirst()
        if (t - poses.first().t < poseMs * 0.8) return null // not watched long enough yet
        val (top, count) = poses.groupingBy { it.pose }.eachCount().maxBy { it.value }.toPair()
        if (count < poses.size * 0.7f || top == firedPose || t < cooldownUntil) return null
        if (top != null && !still()) return null
        firedPose = top
        return top?.let { emit(t, it) }
    }

    /** Every key point within stillSignPalms of where it was at the start of the window. */
    private fun still(): Boolean {
        val from = poses.first()
        return poses.all { s ->
            s.points.size == from.points.size && s.points.indices.step(2).all { i ->
                hypot(s.points[i] - from.points[i], s.points[i + 1] - from.points[i + 1]) <= stillSignPalms * s.palm
            }
        }
    }

    private fun emit(t: Long, g: Gesture): Gesture {
        cooldownUntil = t + cooldownMs
        return g
    }
}
