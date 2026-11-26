package com.lterckski.gesturescroll

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Flicks mode: a quick flick of the pointing index finger.
 * Flick up = next video, down = previous video, to the user's left = next photo, right = previous photo.
 *
 * Feed the index fingertip (landmark 8) in image pixels; the frame isn't mirrored, so the user's left is
 * larger x. Speeds are in palms per second, so distance from the camera doesn't matter.
 */
class FlickDetector(
    @Volatile var cooldownMs: Long = 1000,
    val minSpeed: Float = 4f,
    private val minTravelPalms: Float = 0.6f,
    private val windowMs: Long = 200,
    private val minSpanMs: Long = 60, // at least a couple of frames, so one jumpy landmark isn't a flick
    private val stillSpeed: Float = 1f,
    private val stillMs: Long = 150,
    private val gapMs: Long = 300,
) {
    private class Sample(val t: Long, val x: Float, val y: Float)

    /** Fingertip speed over the last ~0.2 s, palms per second; for the practice readout. */
    @Volatile var speed = 0f
        private set
    @Volatile var cooldownUntil = 0L
        private set
    var lastFlick: Gesture? = null
        private set

    private val samples = ArrayDeque<Sample>()
    private var lastSeenAt = -1L
    private var stillSince = -1L
    // After a flick, the finger coming back reads as the opposite flick; that only counts again once it has rested.
    private var reverseArmed = true

    /** pointing: index extended, the other fingers curled. Other frames are skipped (a fast flick can blur one). */
    fun feed(t: Long, x: Float, y: Float, palm: Float, pointing: Boolean): Gesture? {
        if (!pointing) return null
        if (t - lastSeenAt > gapMs) { samples.clear(); stillSince = -1 }
        lastSeenAt = t
        val nx = x / palm; val ny = y / palm
        samples.lastOrNull()?.let { prev ->
            val instant = hypot(nx - prev.x, ny - prev.y) * 1000 / (t - prev.t).coerceAtLeast(1)
            stillSince = if (instant < stillSpeed) (if (stillSince < 0) prev.t else stillSince) else -1
        }
        if (!reverseArmed && stillSince >= 0 && t - stillSince >= stillMs) reverseArmed = true
        samples.addLast(Sample(t, nx, ny))
        while (t - samples.first().t > windowMs) samples.removeFirst()

        val first = samples.first()
        val span = t - first.t
        val dx = nx - first.x; val dy = ny - first.y
        val travel = hypot(dx, dy)
        speed = if (span > 0) travel * 1000 / span else 0f
        if (t < cooldownUntil || span < minSpanMs || speed < minSpeed || travel < minTravelPalms) return null

        val g = if (abs(dy) >= abs(dx)) { if (dy < 0) Gesture.SWIPE_UP else Gesture.SWIPE_DOWN }
                else { if (dx > 0) Gesture.PHOTO_NEXT else Gesture.PHOTO_PREV }
        if (!reverseArmed && g == opposite(lastFlick)) return null
        samples.clear(); samples.addLast(Sample(t, nx, ny))
        cooldownUntil = t + cooldownMs
        lastFlick = g; reverseArmed = false; stillSince = -1
        return g
    }

    private fun opposite(g: Gesture?) = when (g) {
        Gesture.SWIPE_UP -> Gesture.SWIPE_DOWN
        Gesture.SWIPE_DOWN -> Gesture.SWIPE_UP
        Gesture.PHOTO_NEXT -> Gesture.PHOTO_PREV
        Gesture.PHOTO_PREV -> Gesture.PHOTO_NEXT
        else -> null
    }
}
