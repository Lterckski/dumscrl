package com.lterckski.gesturescroll

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.hypot

/** Runs the front camera + hand and/or face landmarker in the background and reports gestures. */
class GestureService : LifecycleService() {
    companion object {
        @Volatile var status = "Stopped"
            private set
        /** Set by the visible activity; called on the main thread. */
        var listener: ((String) -> Unit)? = null
        /** Set by the visible activity: each frame, its hand points (x0,y0,x1,y1… in frame pixels) and live numbers. */
        var previewListener: ((Int, Int, FloatArray?, String) -> Unit)? = null
        @Volatile private var instance: GestureService? = null
        /** The visible activity's PreviewView; the camera shows straight into it, without waiting for detection. */
        var previewSurface: Preview.SurfaceProvider? = null
            set(value) { field = value; instance?.preview?.setSurfaceProvider(value) } // main thread
        val MODES = listOf("signs", "flicks", "head", "signs+head")
        fun mode(c: Context) = c.getSharedPreferences("settings", MODE_PRIVATE).getString("mode", null).takeIf { it in MODES } ?: "signs"
        private const val CHANNEL = "gesture"
        private const val NOTIF_ID = 1
    }

    private val analysisThread = Executors.newSingleThreadExecutor()
    private val headDetector = GestureDetector()
    private lateinit var mode: String
    private val handDetector by lazy { HandDetector(swipes = mode == "flicks") }
    private var faces: FaceLandmarker? = null
    private var hands: HandLandmarker? = null
    private var paused = false
    // Once-a-second tuning log: adb logcat -s GestureScroll
    private var frames = 0
    private var logAt = 0L
    private var detectMs = 0L
    private var ageMs = 0L
    private var preview: Preview? = null
    private var lastHand = "no hand"
    private var lastPoints: FloatArray? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Gesture detector", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(NOTIF_ID, notification("Watching for gestures"), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        publish("Watching for gestures")

        mode = mode(this)
        instance = this
        analysisThread.execute {
            if (mode != "head") hands = try { handLandmarker(Delegate.GPU) } catch (e: Exception) {
                Log.w("GestureScroll", "GPU hand tracking unavailable, using CPU", e); handLandmarker(Delegate.CPU)
            }
            if (mode == "head" || mode == "signs+head") faces = FaceLandmarker.createFromOptions(
                this,
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumFaces(1)
                    .build()
            )
        }
        ProcessCameraProvider.getInstance(this).let { future ->
            future.addListener({
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(analysisThread, ::analyze)
                // 4:3 like the analysis frames, so the drawn hand lines up with the picture.
                preview = Preview.Builder()
                    .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build())
                    .build().also { it.setSurfaceProvider(previewSurface) }
                future.get().bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis, preview)
            }, mainExecutor)
        }
    }

    private fun handLandmarker(delegate: Delegate): HandLandmarker = HandLandmarker.createFromOptions(
        this,
        HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").setDelegate(delegate).build())
            .setRunningMode(RunningMode.VIDEO)
            .setNumHands(1)
            // Low confidences keep the hand tracked through motion blur during a flick.
            .setMinHandDetectionConfidence(0.3f)
            .setMinHandPresenceConfidence(0.3f)
            .setMinTrackingConfidence(0.3f)
            .build()
    )

    private fun analyze(image: ImageProxy) {
        val t = image.imageInfo.timestamp / 1_000_000
        val bitmap = image.use {
            val raw = it.toBitmap()
            val m = Matrix().apply { postRotate(it.imageInfo.rotationDegrees.toFloat()) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        }
        val w = bitmap.width.toFloat(); val h = bitmap.height.toFloat()
        val mp = BitmapImageBuilder(bitmap).build()
        val started = SystemClock.uptimeMillis()
        hands?.let { handGesture(it, mp, t, w, h) }?.let(::onGesture)
        faces?.let { headGesture(it, mp, t, w, h) }?.let(::onGesture)
        detectMs += SystemClock.uptimeMillis() - started
        // Camera timestamps are on the realtime clock on most phones; this is the frame's age when its result is ready.
        ageMs += SystemClock.elapsedRealtime() - t
        if (previewListener != null) {
            val pts = lastPoints
            val info = lastHand + (if (mode == "flicks") "\nswipe travel ${"%.2f".format(handDetector.travel)} of ${handDetector.minTravelPalms} palms" else "") +
                if (paused) "\nPAUSED" else ""
            mainExecutor.execute { previewListener?.invoke(w.toInt(), h.toInt(), pts, info) }
        }
        frames++
        if (t - logAt >= 1000) {
            Log.d("GestureScroll", "${frames * 1000 / (t - logAt).coerceAtLeast(1)} detections/s, detect ${detectMs / frames} ms, " +
                "frame age ${ageMs / frames} ms, ${w.toInt()}x${h.toInt()}, $lastHand, paused=$paused")
            frames = 0; logAt = t; detectMs = 0; ageMs = 0
        }
    }

    private fun handGesture(lm: HandLandmarker, mp: MPImage, t: Long, w: Float, h: Float): Gesture? {
        val hand = lm.detectForVideo(mp, t).landmarks().firstOrNull()
        if (hand == null) { handDetector.lost(); lastHand = "no hand"; lastPoints = null; return null }
        val px = FloatArray(21) { hand[it].x() * w }
        val py = FloatArray(21) { hand[it].y() * h }
        lastPoints = FloatArray(42) { if (it % 2 == 0) px[it / 2] else py[it / 2] }
        val palm = hypot(px[0] - px[9], py[0] - py[9])
        val shape = classifyHand(px, py)
        // Flick mode keeps thumbs up as its only sign.
        val pose = if (mode == "flicks" && shape.pose != Gesture.THUMBS_UP) null else shape.pose
        val cx = (px[0] + px[5] + px[9] + px[17]) / 4
        val cy = (py[0] + py[5] + py[9] + py[17]) / 4
        lastHand = "hand: ${shape.name} (${shape.fingers} fingers${if (shape.thumbOut) " + thumb" else ""}) palm=${palm.toInt()}px at ${cx.toInt()},${cy.toInt()}"
        // shortcut: a hand close to the lens has a huge palm, so cap the unit at 1/5 of the frame; tune on the phone.
        return handDetector.feed(t, cx, cy, minOf(palm, h / 5), shape.open, pose)
    }

    private fun headGesture(lm: FaceLandmarker, mp: MPImage, t: Long, w: Float, h: Float): Gesture? {
        val face = lm.detectForVideo(mp, t).faceLandmarks().firstOrNull()
        if (face == null) { headDetector.reset(); return null }
        val (pitch, roll) = headPose(face, w, h)
        return headDetector.feed(t, pitch, roll)
    }

    private fun onGesture(g: Gesture) {
        if (g == Gesture.TOGGLE) paused = !paused
        Log.d("GestureScroll", "gesture $g${if (paused && g != Gesture.TOGGLE) " (ignored, paused)" else ""}")
        if (paused && g != Gesture.TOGGLE) return
        val label = when (g) {
            Gesture.SWIPE_UP -> "Next video"
            Gesture.SWIPE_DOWN -> "Previous video"
            Gesture.SWIPE_LEFT -> "Next app"
            Gesture.SWIPE_RIGHT -> "Previous app"
            Gesture.THUMBS_UP -> "Like"
            Gesture.PHOTO_NEXT -> "Next photo (swipe left)"
            Gesture.PHOTO_PREV -> "Previous photo (swipe right)"
            Gesture.NOD -> "Nod (scroll)"
            Gesture.DOUBLE_NOD -> "Double nod (like)"
            Gesture.TILT_LEFT -> "Tilt left (previous app)"
            Gesture.TILT_RIGHT -> "Tilt right (next app)"
            Gesture.TOGGLE -> if (paused) "Paused: hold an open palm still 2 s to resume" else "Resumed"
        }
        mainExecutor.execute {
            publish(label)
            if (g == Gesture.TOGGLE) Toast.makeText(this, if (paused) "dumscrl paused" else "dumscrl resumed", Toast.LENGTH_SHORT).show()
            else GestureAccessibilityService.instance?.perform(g)
        }
    }

    /** pitch/roll as GestureDetector expects, from eye corners (33 = user's right, 263 = user's left) and nose tip (1). */
    private fun headPose(f: List<NormalizedLandmark>, w: Float, h: Float): Pair<Float, Float> {
        val rx = f[33].x() * w; val ry = f[33].y() * h
        val lx = f[263].x() * w; val ly = f[263].y() * h
        val nx = f[1].x() * w; val ny = f[1].y() * h
        val ex = lx - rx; val ey = ly - ry
        val eyeWidth = hypot(ex, ey)
        // Camera frames aren't mirrored, so a tilt to the user's left drops their left eye (y grows).
        val roll = Math.toDegrees(atan2(ey, ex).toDouble()).toFloat()
        // Nose distance below the eye line, perpendicular to it, so tilting doesn't read as nodding.
        val pitch = ((nx - (rx + lx) / 2) * -ey + (ny - (ry + ly) / 2) * ex) / (eyeWidth * eyeWidth)
        return pitch to roll
    }

    private fun publish(text: String) {
        status = text
        listener?.invoke(text)
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification(text))
    }

    private fun notification(text: String): Notification =
        Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("dumscrl")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            )
            .build()

    override fun onDestroy() {
        instance = null
        status = "Stopped"
        listener?.invoke(status)
        analysisThread.execute { hands?.close(); faces?.close() }
        analysisThread.shutdown()
        super.onDestroy()
    }
}
