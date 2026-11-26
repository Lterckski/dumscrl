package com.lterckski.gesturescroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.abs

/** Draws the tracked hand over a FILL_CENTER, mirrored PreviewView of the same 4:3 camera. */
class HandPreview(context: Context) : View(context) {
    private var frameW = 0
    private var frameH = 0
    private var target: FloatArray? = null
    private var shown: FloatArray? = null
    private val dp = resources.displayMetrics.density
    private val shadow = Color.argb(70, 0, 0, 0) // keeps silver readable on bright scenes
    private val bone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(128, 236, 236, 242); strokeWidth = 1.5f * dp; strokeCap = Paint.Cap.ROUND
        setShadowLayer(2f * dp, 0f, 0f, shadow)
    }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(64, 255, 255, 255) }
    private val joint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 244, 248); setShadowLayer(1.5f * dp, 0f, 0f, shadow)
    }
    private val tips = setOf(4, 8, 12, 16, 20)

    fun show(frameW: Int, frameH: Int, points: FloatArray?) {
        this.frameW = frameW; this.frameH = frameH; target = points
        if (points == null || shown?.size != points.size) shown = points?.copyOf() // a hand that just appeared snaps into place
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val t = target ?: return
        val p = shown ?: return
        // Ease toward the latest detection: half the gap per display frame, so jitter softens with ~1 frame of lag.
        var moving = false
        for (i in p.indices) {
            val d = t[i] - p[i]
            p[i] += d * 0.5f
            if (abs(d) > 0.5f) moving = true
        }
        val scale = maxOf(width / frameW.toFloat(), height / frameH.toFloat()) // matches PreviewView FILL_CENTER
        val ox = (width + frameW * scale) / 2
        val oy = (height - frameH * scale) / 2
        fun x(i: Int) = ox - p[i * 2] * scale // the front preview is mirrored like a selfie
        fun y(i: Int) = oy + p[i * 2 + 1] * scale
        for (c in HandLandmarker.HAND_CONNECTIONS) canvas.drawLine(x(c.start()), y(c.start()), x(c.end()), y(c.end()), bone)
        for (i in 0 until 21) {
            val r = (if (i in tips) 4f else 3f) * dp
            canvas.drawCircle(x(i), y(i), r * 2.2f, halo)
            canvas.drawCircle(x(i), y(i), r, joint)
        }
        if (moving) postInvalidateOnAnimation()
    }
}
