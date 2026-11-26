package com.lterckski.gesturescroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction

/** Turns detected gestures into swipes, taps and app switches. Same process as GestureService. */
class GestureAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: GestureAccessibilityService? = null
            private set

        /** Facebook, Instagram, TikTok (two package names depending on region). */
        private val APPS = listOf("com.facebook.katana", "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

        fun isEnabled(c: Context) =
            Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?.contains("${c.packageName}/${GestureAccessibilityService::class.java.name}") == true
    }

    private var currentPackage: String? = null

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) event.packageName?.let { currentPackage = it.toString() }
    }

    /** Call on the main thread. */
    fun perform(g: Gesture) {
        val m = resources.displayMetrics
        val x = m.widthPixels / 2f
        when (g) {
            Gesture.SWIPE_UP, Gesture.NOD -> swipe(x, m.heightPixels * 0.7f, x, m.heightPixels * 0.3f)
            Gesture.SWIPE_DOWN -> swipe(x, m.heightPixels * 0.3f, x, m.heightPixels * 0.7f)
            Gesture.THUMBS_UP, Gesture.DOUBLE_NOD -> doubleTap(x, m.heightPixels / 2f)
            Gesture.SWIPE_LEFT, Gesture.TILT_RIGHT -> switchApp(+1)
            Gesture.SWIPE_RIGHT, Gesture.TILT_LEFT -> switchApp(-1)
            Gesture.PHOTO_NEXT -> if (!scrollCarousel(AccessibilityAction.ACTION_SCROLL_RIGHT, x, m.heightPixels * 0.42f))
                // Short and slow, on the photo: a long fast swipe makes TikTok change tabs (Following / profile) instead.
                swipe(m.widthPixels * 0.68f, m.heightPixels * 0.42f, m.widthPixels * 0.32f, m.heightPixels * 0.42f, 450)
            Gesture.PHOTO_PREV -> if (!scrollCarousel(AccessibilityAction.ACTION_SCROLL_LEFT, x, m.heightPixels * 0.42f))
                swipe(m.widthPixels * 0.32f, m.heightPixels * 0.42f, m.widthPixels * 0.68f, m.heightPixels * 0.42f, 450)
            Gesture.TOGGLE -> {}
        }
    }

    /**
     * Scrolls the innermost horizontally scrollable view under (x, y), e.g. a photo carousel,
     * instead of the page pager around it. Returns false when there is none.
     */
    private fun scrollCarousel(action: AccessibilityAction, x: Float, y: Float): Boolean {
        val root = rootInActiveWindow ?: return false
        var best: AccessibilityNodeInfo? = null
        var bestArea = Long.MAX_VALUE
        val bounds = Rect()
        val stack = ArrayDeque(listOf(root))
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            n.getBoundsInScreen(bounds)
            if (!bounds.contains(x.toInt(), y.toInt())) continue
            val area = bounds.width().toLong() * bounds.height()
            if (action in n.actionList && area < bestArea) { best = n; bestArea = area }
            for (i in 0 until n.childCount) n.getChild(i)?.let(stack::addLast)
        }
        val done = best?.performAction(action.id) == true
        Log.d("GestureScroll", "carousel ${action.label ?: action.id}: ${best?.className ?: "none found"}, scrolled=$done")
        return done
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 250) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, ms)).build(), null, null)
    }

    private fun doubleTap(x: Float, y: Float) {
        val dot = Path().apply { moveTo(x, y) }
        dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(dot, 0, 40))
                .addStroke(GestureDescription.StrokeDescription(dot, 120, 40))
                .build(), null, null
        )
    }

    private fun switchApp(step: Int) {
        val installed = APPS.mapNotNull { pkg -> packageManager.getLaunchIntentForPackage(pkg) }
        if (installed.isEmpty()) return
        val i = installed.indexOfFirst { it.`package` == currentPackage }
        // From any other app, "next" opens the first and "previous" the last.
        val target = installed[if (i < 0) (if (step > 0) 0 else installed.size - 1) else (i + step).mod(installed.size)]
        startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
