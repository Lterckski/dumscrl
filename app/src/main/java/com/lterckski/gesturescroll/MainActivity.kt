package com.lterckski.gesturescroll

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var access: TextView
    private lateinit var handOverlay: HandPreview
    private lateinit var camera: PreviewView
    private lateinit var numbers: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 24f }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(TextView(context).apply { text = "Last gesture"; textSize = 14f })
            addView(status)
            camera = PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
            handOverlay = HandPreview(context)
            addView(FrameLayout(context).apply { addView(camera); addView(handOverlay) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (280 * resources.displayMetrics.density).toInt()))
            numbers = TextView(context).apply { textSize = 13f }
            addView(numbers)
            access = TextView(context).apply { textSize = 16f; setPadding(0, 32, 0, 0) }
            addView(access)
            addView(Button(context).apply {
                text = "Open accessibility settings"
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            })
            addView(TextView(context).apply { text = "\nWatch for"; textSize = 14f })
            addView(RadioGroup(context).apply {
                orientation = RadioGroup.HORIZONTAL
                val current = GestureService.mode(context)
                GestureService.MODES.forEachIndexed { i, m ->
                    addView(RadioButton(context).apply { id = i + 1; text = m.replaceFirstChar(Char::uppercase) })
                    if (m == current) check(i + 1)
                }
                setOnCheckedChangeListener { _, id ->
                    getSharedPreferences("settings", MODE_PRIVATE).edit().putString("mode", GestureService.MODES[id - 1]).apply()
                    // The service picks its models when it starts, so restart it.
                    if (GestureService.status != "Stopped") {
                        stopService(Intent(context, GestureService::class.java)); start()
                    }
                }
            })
            addView(Button(context).apply { text = "Start"; setOnClickListener { start() } })
            addView(Button(context).apply {
                text = "Stop"
                setOnClickListener { stopService(Intent(context, GestureService::class.java)) }
            })
        })
        start()
    }

    override fun onResume() {
        super.onResume()
        // After an app update the system can bring this screen back without onCreate; make sure detection runs.
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && GestureService.status == "Stopped") {
            startForegroundService(Intent(this, GestureService::class.java))
        }
        status.text = GestureService.status
        access.text = if (GestureAccessibilityService.isEnabled(this)) "Phone control: on"
        else "Phone control: off. Turn on dumscrl in accessibility settings so gestures can scroll."
        GestureService.listener = { status.text = it }
        GestureService.previewSurface = camera.surfaceProvider
        GestureService.previewListener = { w, h, points, info ->
            handOverlay.show(w, h, points); numbers.text = info
        }
    }

    override fun onPause() {
        GestureService.listener = null
        GestureService.previewListener = null
        GestureService.previewSurface = null
        super.onPause()
    }

    private fun start() {
        val needed = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isEmpty()) startForegroundService(Intent(this, GestureService::class.java))
        else requestPermissions(needed.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(Intent(this, GestureService::class.java)) // runs without notifications if those were denied
        } else {
            status.text = "Camera permission is needed to see your head."
        }
    }
}
