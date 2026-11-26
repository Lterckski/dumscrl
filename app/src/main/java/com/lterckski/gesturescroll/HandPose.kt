package com.lterckski.gesturescroll

import kotlin.math.abs
import kotlin.math.hypot

/** What one frame's hand looks like. fingers = index..pinky extended; thumb counted separately. */
data class HandShape(val fingers: Int, val thumbOut: Boolean, val open: Boolean, val pose: Gesture?, val name: String)

/**
 * Classifies MediaPipe's 21 hand landmarks (pixels, y down).
 * Finger extension compares distances from the wrist, so it works however the hand is turned.
 */
fun classifyHand(px: FloatArray, py: FloatArray): HandShape {
    fun dist(a: Int, b: Int) = hypot(px[a] - px[b], py[a] - py[b])
    val palm = dist(0, 9)
    val up = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18).map { (tip, pip) -> dist(0, tip) > dist(0, pip) * 1.1f }
    val fingers = up.count { it }
    val thumbOut = dist(4, 9) > 0.7f * palm // tucked across the palm, the thumb tip sits near the middle knuckle
    // Thumbs up is the one sign that needs "up": thumb tip well above its base and the highest point of the hand.
    val thumbsUp = fingers == 0 && py[2] - py[4] > 0.6f * palm && py[4] <= py.min() + 1f
    val (pose, name) = when {
        thumbsUp -> Gesture.THUMBS_UP to "thumbs up"
        // Photos: fist with the thumb sticking out to the user's left (more than 50° from vertical) = swipe left.
        // The frame isn't mirrored, so the user's left is image right (larger x).
        fingers == 0 && thumbOut && dist(2, 4) > 0.6f * palm && abs(px[4] - px[2]) > abs(py[4] - py[2]) * 1.19f ->
            if (px[4] > px[2]) Gesture.PHOTO_NEXT to "thumb left" else null to "thumb right"
        // Only the pinky out, sideways (either way) = swipe right.
        up == listOf(false, false, false, true) && abs(px[20] - px[17]) > abs(py[20] - py[17]) * 1.19f ->
            Gesture.PHOTO_PREV to "pinky"
        up == listOf(true, false, false, false) -> Gesture.SWIPE_UP to "point"
        up == listOf(true, true, false, false) -> Gesture.SWIPE_DOWN to "peace"
        up == listOf(true, true, true, false) -> Gesture.SWIPE_LEFT to "three"
        fingers == 4 && !thumbOut -> Gesture.SWIPE_RIGHT to "four"
        fingers == 4 -> null to "open palm"
        fingers == 0 -> null to "fist"
        else -> null to "other"
    }
    return HandShape(fingers, thumbOut, fingers >= 3, pose, name)
}
