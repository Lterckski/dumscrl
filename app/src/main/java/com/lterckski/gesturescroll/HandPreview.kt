package com.lterckski.gesturescroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

/** Draws the tracked hand over a FIT_CENTER, mirrored PreviewView of the same 4:3 camera. */
class HandPreview(context: Context) : View(context) {
    private var frameW = 0
    private var frameH = 0
    private var points: FloatArray? = null
    private val bone = Paint().apply { color = Color.GREEN; strokeWidth = 4f; isAntiAlias = true }
    private val joint = Paint().apply { color = Color.YELLOW; isAntiAlias = true }

    fun show(frameW: Int, frameH: Int, points: FloatArray?) {
        this.frameW = frameW; this.frameH = frameH; this.points = points; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val p = points ?: return
        val scale = minOf(width / frameW.toFloat(), height / frameH.toFloat())
        canvas.save()
        canvas.translate((width + frameW * scale) / 2, (height - frameH * scale) / 2)
        canvas.scale(-scale, scale) // the front preview is mirrored like a selfie
        for (c in HandLandmarker.HAND_CONNECTIONS) {
            canvas.drawLine(p[c.start() * 2], p[c.start() * 2 + 1], p[c.end() * 2], p[c.end() * 2 + 1], bone)
        }
        for (i in 0 until 21) canvas.drawCircle(p[i * 2], p[i * 2 + 1], 6f / scale + 2f, joint)
        canvas.restore()
    }
}
