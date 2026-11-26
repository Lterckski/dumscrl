package com.lterckski.gesturescroll

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Turns a stream of hand samples into gestures.
 *
 * x, y: palm centre in image pixels (camera frame, not mirrored, so the user's right is smaller x).
 * palm: wrist-to-middle-knuckle length in pixels; all distances are measured in palms,
 * so it works the same near or far from the camera.
 * open: at least three fingers extended. Swipes and the pause hold need an open hand, so a fist or a
 * hand curled around food moving past the camera does nothing.
 *
 * Thresholds are first guesses; calibrate on the real phone.
 */
class HandDetector(
    private val swipes: Boolean = true, // false in sign mode: 3- and 4-finger signs would read as open-hand swipes
    val minTravelPalms: Float = 0.8f,
    private val windowMs: Long = 500,
    private val gapMs: Long = 300,
    private val cooldownMs: Long = 500,
    private val oppositeLockMs: Long = 1000,
    private val settleMs: Long = 200,
    private val holdMs: Long = 2000, // long, so a resting hand rarely pauses by accident
    private val stillPalms: Float = 0.3f,
    private val poseMs: Long = 300,
) {
    private class Sample(val t: Long, val x: Float, val y: Float, val open: Boolean)

    /** Distance moved in the swipe window, in palms; for the on-screen tuning readout. */
    var travel = 0f
        private set

    private val history = ArrayDeque<Sample>()
    private var appearedAt = -1L
    private var lastSeenAt = -1L
    private var cooldownUntil = 0L
    private var lastSwipe: Gesture? = null
    private var lastSwipeAt = 0L
    private var holdStart = -1L
    private var holdX = 0f
    private var holdY = 0f
    private var holdConsumed = false
    private val poses = ArrayDeque<Pair<Long, Gesture?>>()
    private var firedPose: Gesture? = null

    /**
     * Call when no hand is visible. Swipe history is kept: a fast flick often blurs the hand
     * out for a frame or two, and the samples either side still make the swipe.
     */
    fun lost() {
        holdStart = -1; holdConsumed = false
    }

    /** pose: a held hand sign (thumbs up, pointing, peace) and the gesture it stands for, or null. */
    fun feed(t: Long, x: Float, y: Float, palm: Float, open: Boolean, pose: Gesture?): Gesture? {
        if (t - lastSeenAt > gapMs) { // a new appearance, not the same motion or sign
            appearedAt = t; history.clear(); poses.clear(); firedPose = null
        }
        lastSeenAt = t

        signFired(t, pose)?.let { return it }
        if (pose != null) { history.clear(); holdStart = -1; return null }

        // Open palm held still = pause/resume. Fires once per hold.
        if (!open) {
            holdStart = -1; holdConsumed = false
        } else if (holdStart < 0 || hypot(x - holdX, y - holdY) / palm > stillPalms) {
            holdStart = t; holdX = x; holdY = y
        } else if (!holdConsumed && t - holdStart >= holdMs) {
            holdConsumed = true; history.clear()
            return emit(t, Gesture.TOGGLE)
        }

        if (!swipes) return null
        if (t < cooldownUntil) { history.clear(); return null } // the tail of the last swipe isn't a new one
        history.addLast(Sample(t, x, y, open))
        while (t - history.first().t > windowMs) history.removeFirst()
        if (t - appearedAt < settleMs) return null // hand still entering the frame
        val from = history.first()
        // The swipe must start with an open hand, so a fist or a hand around food doesn't scroll;
        // fingers may blur closed mid-flick.
        if (!from.open) return null
        val dx = (x - from.x) / palm
        val dy = (y - from.y) / palm
        travel = maxOf(abs(dx), abs(dy))
        val g = when {
            abs(dy) >= abs(dx) && abs(dy) > minTravelPalms -> if (dy < 0) Gesture.SWIPE_UP else Gesture.SWIPE_DOWN
            abs(dx) > minTravelPalms -> if (dx < 0) Gesture.SWIPE_RIGHT else Gesture.SWIPE_LEFT
            else -> return null
        }
        history.clear()
        // Bringing the hand back after a swipe looks like the opposite swipe: ignore it.
        if (g == opposite(lastSwipe) && t - lastSwipeAt < oppositeLockMs) return null
        lastSwipe = g; lastSwipeAt = t
        return emit(t, g)
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

    private fun opposite(g: Gesture?) = when (g) {
        Gesture.SWIPE_UP -> Gesture.SWIPE_DOWN
        Gesture.SWIPE_DOWN -> Gesture.SWIPE_UP
        Gesture.SWIPE_LEFT -> Gesture.SWIPE_RIGHT
        Gesture.SWIPE_RIGHT -> Gesture.SWIPE_LEFT
        else -> null
    }

    private fun emit(t: Long, g: Gesture): Gesture {
        cooldownUntil = t + cooldownMs
        return g
    }
}
