package com.macrorecorder.app.service.recording

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent

/**
 * Buffers a finger's DOWN → MOVE* → UP sequence and dispatches the complete
 * stroke as a single [GestureDescription] via [dispatch] when UP is received.
 *
 * This allows touches intercepted by the recording overlay to be re-sent to the
 * underlying app via the AccessibilityService, so the user can interact normally
 * while the macro is being recorded.
 *
 * Multi-touch: each pointer ID gets its own stroke buffer. Concurrent strokes
 * are dispatched independently when each pointer is lifted.
 *
 * Limitation (MVP): gestures are only dispatched after the finger is lifted,
 * so there is a delay equal to the gesture's own duration before the underlying
 * app receives the touch. This is acceptable for taps and short swipes.
 *
 * @param dispatch Callback that forwards a built [GestureDescription] to the
 *                 AccessibilityService. The lambda is evaluated at call time,
 *                 so a late-connected service is handled gracefully.
 */
class GestureRedispatcher(private val dispatch: (GestureDescription) -> Unit) {

    private data class StrokeBuffer(
        val points: MutableList<PointF> = mutableListOf(),
        val startMs: Long = System.currentTimeMillis()
    )

    private val buffers = mutableMapOf<Int, StrokeBuffer>()

    /**
     * Feed every [MotionEvent] from the capture overlay into this method.
     * Returns `true` so the caller can use it directly as an [android.view.View.OnTouchListener].
     */
    fun onMotionEvent(event: MotionEvent): Boolean {
        val idx = event.actionIndex
        val pid = event.getPointerId(idx)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                buffers[pid] = StrokeBuffer().also {
                    it.points.add(PointF(event.getX(idx), event.getY(idx)))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // MOVE events report all active pointers — update each one.
                for (i in 0 until event.pointerCount) {
                    val p = event.getPointerId(i)
                    buffers[p]?.points?.add(PointF(event.getX(i), event.getY(i)))
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val buf = buffers.remove(pid) ?: return true
                buf.points.add(PointF(event.getX(idx), event.getY(idx)))
                dispatchStroke(buf)
            }
            MotionEvent.ACTION_CANCEL -> buffers.clear()
        }
        return true
    }

    private fun dispatchStroke(buf: StrokeBuffer) {
        if (buf.points.isEmpty()) return
        val path = Path().apply {
            moveTo(buf.points[0].x, buf.points[0].y)
            buf.points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val durationMs = maxOf(System.currentTimeMillis() - buf.startMs, 1L)
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        dispatch(GestureDescription.Builder().addStroke(stroke).build())
    }
}
