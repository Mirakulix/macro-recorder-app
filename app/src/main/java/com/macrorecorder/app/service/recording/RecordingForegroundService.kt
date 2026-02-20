package com.macrorecorder.app.service.recording

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.lifecycle.LifecycleService
import com.macrorecorder.app.R
import com.macrorecorder.app.domain.model.TouchAction
import com.macrorecorder.app.presentation.main.MainActivity
import com.macrorecorder.app.service.accessibility.MacroRecorderAccessibilityService
import com.macrorecorder.app.util.NotificationHelper

/**
 * Foreground service that owns the entire recording session:
 *  1. Shows a persistent notification so Android keeps the process alive.
 *  2. Acquires a partial wake lock so events aren't missed when the screen dims.
 *  3. Adds a draggable overlay widget ([R.layout.overlay_widget]) via WindowManager.
 *  4. Adds a transparent full-screen capture layer that intercepts touch events,
 *     records them in [RecordingManager], and re-dispatches them to the underlying
 *     app via [GestureRedispatcher] + [MacroRecorderAccessibilityService].
 *  5. On stop: saves the result in [RecordingManager.lastResult] and brings
 *     [MainActivity] to the foreground so the user can name and save the macro.
 */
class RecordingForegroundService : LifecycleService() {

    // ── Companion / static ────────────────────────────────────────────────────

    companion object {
        const val ACTION_START  = "com.macrorecorder.app.RECORDING_START"
        const val ACTION_STOP   = "com.macrorecorder.app.RECORDING_STOP"
        const val ACTION_PAUSE  = "com.macrorecorder.app.RECORDING_PAUSE"
        const val ACTION_RESUME = "com.macrorecorder.app.RECORDING_RESUME"
        const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context) =
            Intent(context, RecordingForegroundService::class.java)
                .apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, RecordingForegroundService::class.java)
                .apply { action = ACTION_STOP }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private lateinit var windowManager: WindowManager
    private var widgetView: View? = null
    private var captureView: View? = null

    // Widget drag tracking
    private var widgetParams: WindowManager.LayoutParams? = null
    private var touchRawX = 0f
    private var touchRawY = 0f
    private var widgetStartX = 0
    private var widgetStartY = 0

    private var wakeLock: PowerManager.WakeLock? = null

    private val redispatcher = GestureRedispatcher { gesture ->
        MacroRecorderAccessibilityService.instance?.dispatchGesture(gesture, null, null)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START  -> startRecording()
            ACTION_STOP   -> stopRecording()
            ACTION_PAUSE  -> { RecordingManager.pause();  updatePauseButton(paused = true) }
            ACTION_RESUME -> { RecordingManager.resume(); updatePauseButton(paused = false) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlays()
        releaseWakeLock()
        if (RecordingManager.isRecording) RecordingManager.stop()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    // ── Recording control ─────────────────────────────────────────────────────

    private fun startRecording() {
        NotificationHelper.createChannels(this)
        startForeground(NOTIFICATION_ID, NotificationHelper.buildRecordingNotification(this))
        acquireWakeLock()
        RecordingManager.start()
        addWidgetOverlay()
        addCaptureOverlay()
    }

    private fun stopRecording() {
        RecordingManager.stop()         // result stored in RecordingManager.lastResult
        removeOverlays()
        releaseWakeLock()

        // Bring MainActivity forward to show the save dialog
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_SHOW_SAVE_DIALOG
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        stopSelf()
    }

    // ── Overlay widget ────────────────────────────────────────────────────────

    private fun addWidgetOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }
        widgetParams = params

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_widget, null)

        // Drag via the handle strip at the top of the widget
        view.findViewById<View>(R.id.dragHandle).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchRawX  = event.rawX;  touchRawY  = event.rawY
                    widgetStartX = params.x;  widgetStartY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = widgetStartX + (event.rawX - touchRawX).toInt()
                    params.y = widgetStartY + (event.rawY - touchRawY).toInt()
                    widgetView?.let { windowManager.updateViewLayout(it, params) }
                }
            }
            true
        }

        // Stop button
        view.findViewById<ImageButton>(R.id.btnStop).setOnClickListener {
            stopRecording()
        }

        // Pause / resume toggle
        view.findViewById<ImageButton>(R.id.btnPause).setOnClickListener {
            if (RecordingManager.isPaused) {
                RecordingManager.resume()
                updatePauseButton(paused = false)
            } else {
                RecordingManager.pause()
                updatePauseButton(paused = true)
            }
        }

        windowManager.addView(view, params)
        widgetView = view
    }

    private fun updatePauseButton(paused: Boolean) {
        widgetView?.findViewById<ImageButton>(R.id.btnPause)
            ?.setImageResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause)
    }

    // ── Capture overlay ───────────────────────────────────────────────────────

    private fun addCaptureOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val view = View(this)
        view.setOnTouchListener { _, event ->
            if (!RecordingManager.isRecording || RecordingManager.isPaused) {
                return@setOnTouchListener false // pass through when not recording
            }

            val idx = event.actionIndex
            val action = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> TouchAction.DOWN
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP   -> TouchAction.UP
                MotionEvent.ACTION_MOVE          -> TouchAction.MOVE
                MotionEvent.ACTION_CANCEL        -> TouchAction.CANCEL
                else                             -> null
            }
            if (action != null) {
                RecordingManager.addEvent(
                    x         = event.getX(idx),
                    y         = event.getY(idx),
                    action    = action,
                    pressure  = event.getPressure(idx),
                    pointerId = event.getPointerId(idx)
                )
                redispatcher.onMotionEvent(event)
            }
            true // consume — gesture is re-dispatched via AccessibilityService
        }

        windowManager.addView(view, params)
        captureView = view
    }

    private fun removeOverlays() {
        widgetView?.let  { runCatching { windowManager.removeView(it) };  widgetView  = null }
        captureView?.let { runCatching { windowManager.removeView(it) };  captureView = null }
    }

    // ── Wake lock ─────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MacroRecorder::RecordingWakeLock"
        ).also { it.acquire(30 * 60 * 1000L) } // 30-minute safety cap
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}
