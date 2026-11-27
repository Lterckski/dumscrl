package com.lterckski.gesturescroll

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Flicks mode: a quick flick of the index finger, or of index and middle together; the thumb doesn't matter.
 * Up or down, the fingers pick the action: index = next video, index + middle = previous video.
 * Sideways, index only: to the user's left = next photo, right = previous photo.
 *
 * Feed the index fingertip (landmark 8) in image pixels; the frame isn't mirrored, so the user's left is
 * larger x. Speeds are in palms per second, so distance from the camera doesn't matter.
 */
class FlickDetector(
    @Volatile var cooldownMs: Long = 600,
    val minSpeed: Float = 4f,
    private val minTravelPalms: Float = 0.6f,
    private val windowMs: Long = 200,
    private val minSpanMs: Long = 60, // at least a couple of frames, so one jumpy landmark isn't a flick
    private val stillSpeed: Float = 1.5f, // over ~0.1 s: landmark jitter frame to frame alone is ~1 palm/s
    private val stillMs: Long = 150,
    private val gapMs: Long = 300,
    private val poseMs: Long = 400,
    private val minPoseFrames: Int = 3,
) {
    private class Sample(val t: Long, val x: Float, val y: Float, val fingers: Int?)

    /** Fingertip speed over the last ~0.2 s, palms per second; for the practice readout. */
    @Volatile var speed = 0f
        private set
    @Volatile var cooldownUntil = 0L
        private set
    var lastFlick: Gesture? = null
        private set

    private val samples = ArrayDeque<Sample>()
    private val poses = ArrayDeque<Pair<Long, Int>>()
    /** For the tuning log: whether the opposite of the last flick would count now. */
    val reverseReady get() = reverseArmed
    private var lastSeenAt = -1L
    private var stillSince = -1L
    // After a flick, the finger coming back reads as the opposite flick; that only counts again once it has rested.
    private var reverseArmed = true

    /**
     * fingers: 1 = index up, 2 = index and middle up (the rest curled, thumb ignored), null = neither.
     * Every frame counts for speed: mid-flick the fingers often blur or curl and read as a fist. Which flick it is
     * comes from the pointing frames of the last ~0.4 s.
     */
    fun feed(t: Long, x: Float, y: Float, palm: Float, fingers: Int?): Gesture? {
        if (t - lastSeenAt > gapMs) { samples.clear(); poses.clear(); stillSince = -1 }
        if (fingers != null) poses.addLast(t to fingers)
        while (poses.isNotEmpty() && t - poses.first().first > poseMs) poses.removeFirst()
        lastSeenAt = t
        val nx = x / palm; val ny = y / palm
        samples.firstOrNull { t - it.t <= 100 }?.let { prev ->
            val recent = hypot(nx - prev.x, ny - prev.y) * 1000 / (t - prev.t).coerceAtLeast(1)
            stillSince = if (recent < stillSpeed) (if (stillSince < 0) prev.t else stillSince) else -1
        }
        if (!reverseArmed && stillSince >= 0 && t - stillSince >= stillMs) reverseArmed = true
        samples.addLast(Sample(t, nx, ny, fingers))
        while (t - samples.first().t > windowMs) samples.removeFirst()

        val first = samples.first()
        val span = t - first.t
        val dx = nx - first.x; val dy = ny - first.y
        val travel = hypot(dx, dy)
        speed = if (span > 0) travel * 1000 / span else 0f
        if (t < cooldownUntil || span < minSpanMs || speed < minSpeed || travel < minTravelPalms) return null

        // The way the finger moved; bringing it back is the opposite move and only counts after a rest.
        val move = if (abs(dy) >= abs(dx)) (if (dy < 0) UP else DOWN) else (if (dx > 0) LEFT else RIGHT)
        if (!reverseArmed && move == opposite(lastMove)) return null
        if (poses.size < minPoseFrames) return null // no pointing hand just now: a fist or open hand moving
        val two = poses.count { it.second == 2 } * 2 > poses.size
        val g = when (move) {
            UP, DOWN -> if (two) Gesture.SWIPE_DOWN else Gesture.SWIPE_UP
            else -> if (two) return null else if (move == LEFT) Gesture.PHOTO_NEXT else Gesture.PHOTO_PREV
        }
        samples.clear(); samples.addLast(Sample(t, nx, ny, fingers))
        cooldownUntil = t + cooldownMs
        lastFlick = g; lastMove = move; reverseArmed = false; stillSince = -1
        return g
    }

    private var lastMove = 0
    private fun opposite(m: Int) = when (m) { UP -> DOWN; DOWN -> UP; LEFT -> RIGHT; RIGHT -> LEFT; else -> 0 }

    private companion object {
        const val UP = 1; const val DOWN = 2; const val LEFT = 3; const val RIGHT = 4 // the user's left = image +x
    }
}
