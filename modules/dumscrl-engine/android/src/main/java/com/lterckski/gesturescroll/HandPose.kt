package com.lterckski.gesturescroll

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot

/** What one frame's hand looks like. fingers = index..pinky extended; thumb counted separately. */
data class HandShape(val fingers: Int, val thumbOut: Boolean, val open: Boolean, val pose: Gesture?, val name: String, val up: List<Boolean> = emptyList()) {
    /**
     * Flicks mode's finger poses, thumb ignored: 1 = index only, 2 = index and middle, else null.
     * sideways: the index points sideways. Then ring and pinky hide behind the other two and the tracker often
     * guesses them straight, so only index and middle are read.
     */
    fun flickFingers(sideways: Boolean): Int? = when {
        up.size != 4 || !up[0] -> null
        sideways -> if (up[1]) 2 else 1
        up == listOf(true, false, false, false) -> 1
        up == listOf(true, true, false, false) -> 2
        else -> null
    }
}

/**
 * Classifies MediaPipe's 21 hand landmarks (pixels, y down).
 * Finger extension compares distances from the wrist, so it works however the hand is turned.
 */
fun classifyHand(px: FloatArray, py: FloatArray): HandShape {
    fun dist(a: Int, b: Int) = hypot(px[a] - px[b], py[a] - py[b])
    /** Angle at joint b, in degrees: 180 = straight. */
    fun bend(a: Int, b: Int, c: Int): Double {
        val ux = px[a] - px[b]; val uy = py[a] - py[b]; val vx = px[c] - px[b]; val vy = py[c] - py[b]
        return Math.toDegrees(acos(((ux * vx + uy * vy) / (hypot(ux, uy) * hypot(vx, vy)).coerceAtLeast(1e-6f)).coerceIn(-1f, 1f).toDouble()))
    }
    val palm = dist(0, 9)
    val up = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18).map { (tip, pip) -> dist(0, tip) > dist(0, pip) * 1.1f }
    val fingers = up.count { it }
    val thumbOut = dist(4, 9) > 0.7f * palm // tucked across the palm, the thumb tip sits near the middle knuckle
    // Thumbs up is the one sign that needs "up": thumb tip well above its base and the highest point of the hand.
    val thumbsUp = fingers == 0 && py[2] - py[4] > 0.6f * palm && py[4] <= py.min() + 1f
    // Raised thumb, for counting: straight, and its tip well clear of the index knuckle (a tucked thumb sits on it).
    val thumbRaised = dist(4, 5) > 0.55f * palm && bend(2, 3, 4) > 140 && dist(0, 4) > dist(0, 3)
    val raised = fingers + if (thumbRaised) 1 else 0
    val (pose, name) = when {
        thumbsUp -> Gesture.THUMBS_UP to "thumbs up"
        // Photos: fist with the thumb sticking out to the user's left (more than 50° from vertical) = swipe left.
        // The frame isn't mirrored, so the user's left is image right (larger x).
        // The thumb has to be straight and clear of the curled index, not a relaxed thumb that swings sideways
        // as the wrist turns.
        fingers == 0 && thumbOut && dist(2, 4) > 0.6f * palm && bend(2, 3, 4) > 150 && dist(4, 6) > 0.6f * palm &&
            abs(px[4] - px[2]) > abs(py[4] - py[2]) * 1.19f ->
            if (px[4] > px[2]) Gesture.PHOTO_NEXT to "thumb left" else null to "thumb right"
        // Only the pinky out, sideways (either way) = swipe right.
        up == listOf(false, false, false, true) && abs(px[20] - px[17]) > abs(py[20] - py[17]) * 1.19f ->
            Gesture.PHOTO_PREV to "pinky"
        up == listOf(true, false, false, false) -> Gesture.SWIPE_UP to "point"
        up == listOf(true, true, false, false) && !thumbRaised -> Gesture.SWIPE_DOWN to "peace"
        // Any three or four raised fingers, the thumb included, whichever ones.
        raised == 3 -> Gesture.SWIPE_LEFT to "three"
        raised == 4 -> Gesture.SWIPE_RIGHT to "four"
        raised == 5 -> null to "open palm"
        fingers == 0 -> null to "fist"
        else -> null to "other"
    }
    return HandShape(fingers, thumbOut, fingers >= 3, pose, name, up)
}

/**
 * Signs that work in each mode; the modes don't overlap. Flicks already scroll and flip photos, so in flicks mode
 * only like (thumbs up) and next/previous app (three, four fingers) stay. The open-palm pause isn't a pose; it works in both.
 */
fun signAllowed(mode: String, pose: Gesture?): Boolean =
    mode != "flicks" || pose in setOf(Gesture.THUMBS_UP, Gesture.SWIPE_LEFT, Gesture.SWIPE_RIGHT)

/** Per finger (thumb..pinky): tip/PIP distance-from-wrist ratio and the bend at PIP and DIP, for tuning logs. */
fun fingerReadout(px: FloatArray, py: FloatArray): String {
    fun dist(a: Int, b: Int) = hypot(px[a] - px[b], py[a] - py[b])
    fun bend(a: Int, b: Int, c: Int): Int {
        val ux = px[a] - px[b]; val uy = py[a] - py[b]; val vx = px[c] - px[b]; val vy = py[c] - py[b]
        return Math.toDegrees(acos(((ux * vx + uy * vy) / (hypot(ux, uy) * hypot(vx, vy)).coerceAtLeast(1e-6f)).coerceIn(-1f, 1f).toDouble())).toInt()
    }
    return listOf("T" to 1, "I" to 5, "M" to 9, "R" to 13, "P" to 17).joinToString(" ") { (n, b) ->
        "$n ${"%.2f".format(dist(0, b + 3) / dist(0, b + 1))} ${bend(b, b + 1, b + 2)}/${bend(b + 1, b + 2, b + 3)}"
    }
}
