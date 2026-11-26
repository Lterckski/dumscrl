package com.lterckski.gesturescroll

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.camera.view.PreviewView
import android.widget.FrameLayout
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.functions.Queues
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.views.ExpoView

/** JavaScript side of the gesture engine: start/stop, mode, pause, phone-control status, events and the preview view. */
class DumscrlEngineModule : Module() {
  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  private fun prefs() = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

  override fun definition() = ModuleDefinition {
    Name("DumscrlEngine")
    Events("onStatus", "onSign", "onFlickMeter")

    OnStartObserving {
      GestureService.listener = { sendEvent("onStatus", mapOf("label" to it)) }
      GestureService.signListener = { sign, paused -> sendEvent("onSign", mapOf("sign" to sign, "paused" to paused)) }
      GestureService.meterListener = { speed, threshold, left, total, last, at ->
        sendEvent("onFlickMeter", mapOf("speed" to speed, "threshold" to threshold, "cooldownLeft" to left, "cooldown" to total, "last" to last, "at" to at))
      }
    }
    OnStopObserving {
      GestureService.listener = null
      GestureService.signListener = null
      GestureService.meterListener = null
    }

    // Camera permission must be granted first (asked from JavaScript).
    Function("start") {
      GestureService.stopRequested = false
      context.startForegroundService(Intent(context, GestureService::class.java)); Unit
    }
    // On while the Camera tab is open: signs are read and shown, but nothing is performed on the phone.
    Function("setPractice") { on: Boolean -> GestureService.practice = on }
    // A service that hasn't reached onCreate yet stops itself there instead.
    Function("stop") {
      if (GestureService.running) context.stopService(Intent(context, GestureService::class.java)) else GestureService.stopRequested = true
      Unit
    }
    Function("isRunning") { GestureService.running }
    Function("status") { GestureService.status }

    Function("getMode") { GestureService.mode(context) }
    // The service picks its models when it starts, so a running one is restarted.
    Function("setMode") { mode: String ->
      require(mode in GestureService.MODES) { "Unknown mode $mode" }
      context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("mode", mode).apply()
      if (GestureService.running) {
        context.stopService(Intent(context, GestureService::class.java))
        context.startForegroundService(Intent(context, GestureService::class.java))
      }
    }

    Function("getFlickCooldown") { GestureService.flickCooldownMs(context) }
    Function("setFlickCooldown") { ms: Long -> GestureService.setFlickCooldownMs(context, ms.coerceIn(300, 2000)) }

    Function("isPaused") { GestureService.isPaused }
    AsyncFunction("setPaused") { paused: Boolean -> GestureService.setPaused(paused) }.runOnQueue(Queues.MAIN)

    Function("isAccessibilityEnabled") { GestureAccessibilityService.isEnabled(context) }
    Function("openAccessibilitySettings") {
      context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // "system", "light" or "dark"; kept natively so it's there before the first frame.
    Function("getTheme") { prefs().getString("theme", "system") }
    Function("setTheme") { theme: String -> prefs().edit().putString("theme", theme).apply() }

    View(DumscrlPreviewView::class) {}
  }
}

/** Live front-camera preview with the tracked hand drawn on top. Only one can be on screen at a time. */
class DumscrlPreviewView(context: Context, appContext: AppContext) : ExpoView(context, appContext) {
  override val shouldUseAndroidLayout = true
  private val camera = PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
  private val handOverlay = HandPreview(context)

  init {
    addView(FrameLayout(context).apply { addView(camera); addView(handOverlay) }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    GestureService.previewSurface = camera.surfaceProvider
    GestureService.previewListener = { w, h, points, _ -> handOverlay.show(w, h, points) }
  }

  override fun onDetachedFromWindow() {
    if (GestureService.previewSurface === camera.surfaceProvider) {
      GestureService.previewSurface = null
      GestureService.previewListener = null
    }
    super.onDetachedFromWindow()
  }
}
