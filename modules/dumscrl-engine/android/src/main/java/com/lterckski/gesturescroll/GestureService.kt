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
        /** Status and gesture labels, on the main thread. */
        var listener: ((String) -> Unit)? = null
        /** The sign currently seen ("point", "fist", "no hand"…) and whether paused; on the main thread, on change only. */
        var signListener: ((String, Boolean) -> Unit)? = null
        val running get() = instance != null
        /** Practice (the Camera tab is open): signs are read and shown, but nothing happens on the phone. */
        @Volatile var practice = false
        /** Stop asked for before onCreate ran; stopping then would crash (startForeground never called). */
        @Volatile var stopRequested = false
        val isPaused get() = instance?.paused ?: false
        /** Main thread. */
        fun setPaused(p: Boolean) {
            val s = instance ?: return
            if (s.paused != p) { s.paused = p; s.publish(if (p) "Paused" else "Resumed") }
        }
        /** Set by the visible activity: each frame, its hand points (x0,y0,x1,y1… in frame pixels) and live numbers. */
        var previewListener: ((Int, Int, FloatArray?, String) -> Unit)? = null
        /** Flicks mode, while a preview is showing, ~10 times a second: speed, threshold, cooldown left/total (ms), last flick and when. */
        var meterListener: ((Float, Float, Long, Long, String?, Long) -> Unit)? = null
        fun flickCooldownMs(c: Context) = c.getSharedPreferences("settings", MODE_PRIVATE).getLong("flickCooldownMs", 1000)
        /** Saved, and applied to a running engine straight away. */
        fun setFlickCooldownMs(c: Context, ms: Long) {
            c.getSharedPreferences("settings", MODE_PRIVATE).edit().putLong("flickCooldownMs", ms).apply()
            instance?.flicks?.cooldownMs = ms
        }
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
    private val handDetector = HandDetector()
    private val flicks by lazy { FlickDetector(cooldownMs = flickCooldownMs(this)) }
    private var meterAt = 0L
    private var faces: FaceLandmarker? = null
    private var hands: HandLandmarker? = null
    @Volatile private var paused = false
    private var lastSign = ""
    private var lastSignPaused = false
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
        mode = mode(this)
        instance = this // before publishing, so the app reads running = true
        publish("Watching for gestures")
        if (stopRequested) { stopSelf(); return }
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
            val info = lastHand + (if (mode == "flicks") "\nfingertip ${"%.1f".format(flicks.speed)} of ${flicks.minSpeed} palms/s" else "") +
                if (paused) "\nPAUSED" else ""
            mainExecutor.execute { previewListener?.invoke(w.toInt(), h.toInt(), pts, info) }
        }
        if (mode == "flicks" && previewListener != null && t - meterAt >= 100) {
            meterAt = t
            val left = (flicks.cooldownUntil - t).coerceAtLeast(0)
            val last = flicks.lastFlick?.let { label(it) }
            val at = flicks.cooldownUntil - flicks.cooldownMs
            val speed = flicks.speed
            mainExecutor.execute { meterListener?.invoke(speed, flicks.minSpeed, left, flicks.cooldownMs, last, at) }
        }
        val sign = lastHand.substringAfter("hand: ", "no hand").substringBefore(" (")
        if (sign != lastSign || paused != lastSignPaused) {
            lastSign = sign; lastSignPaused = paused
            val p = paused
            mainExecutor.execute { signListener?.invoke(sign, p) }
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
        // Flicks mode: a pointing finger is for flicking, so the index-up sign is off; the other signs still work.
        val pose = if (mode == "flicks" && shape.name == "point") null else shape.pose
        val cx = (px[0] + px[5] + px[9] + px[17]) / 4
        val cy = (py[0] + py[5] + py[9] + py[17]) / 4
        lastHand = "hand: ${shape.name} (${shape.fingers} fingers${if (shape.thumbOut) " + thumb" else ""}) palm=${palm.toInt()}px at ${cx.toInt()},${cy.toInt()}"
        // shortcut: a hand close to the lens has a huge palm, so cap the unit at 1/5 of the frame; tune on the phone.
        val unit = minOf(palm, h / 5)
        val flick = if (mode == "flicks") flicks.feed(t, px[8], py[8], unit, shape.name == "point") else null
        return flick ?: handDetector.feed(t, cx, cy, unit, shape.open, pose)
    }

    private fun headGesture(lm: FaceLandmarker, mp: MPImage, t: Long, w: Float, h: Float): Gesture? {
        val face = lm.detectForVideo(mp, t).faceLandmarks().firstOrNull()
        if (face == null) { headDetector.reset(); return null }
        val (pitch, roll) = headPose(face, w, h)
        return headDetector.feed(t, pitch, roll)
    }

    private fun onGesture(g: Gesture) {
        if (practice) {
            Log.d("GestureScroll", "gesture $g (test mode, not performed)")
            mainExecutor.execute { publish("Test: ${if (g == Gesture.TOGGLE) "Pause or resume" else label(g)}") }
            return
        }
        if (g == Gesture.TOGGLE) paused = !paused
        Log.d("GestureScroll", "gesture $g${if (paused && g != Gesture.TOGGLE) " (ignored, paused)" else ""}")
        if (paused && g != Gesture.TOGGLE) return
        val label = label(g)
        mainExecutor.execute {
            publish(label)
            if (g == Gesture.TOGGLE) Toast.makeText(this, if (paused) "dumscrl paused" else "dumscrl resumed", Toast.LENGTH_SHORT).show()
            else GestureAccessibilityService.instance?.perform(g)
        }
    }

    private fun label(g: Gesture) = when (g) {
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
                PendingIntent.getActivity(this, 0, packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IMMUTABLE)
            )
            .build()

    override fun onDestroy() {
        instance = null
        status = "Stopped"
        listener?.invoke(status)
        // Null them too: frames already queued behind this would otherwise run on a closed landmarker (native crash).
        analysisThread.execute { hands?.close(); faces?.close(); hands = null; faces = null }
        analysisThread.shutdown()
        super.onDestroy()
    }
}
