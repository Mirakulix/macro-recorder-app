package com.macrorecorder.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.macrorecorder.app.domain.model.TouchAction
import com.macrorecorder.app.domain.model.TouchEvent

/**
 * Renders a touch-event sequence as a path diagram on a dark canvas.
 *
 * Coordinates are normalised to the fixed output size, so the result looks good
 * regardless of the recording device's screen resolution. Each pointer (finger)
 * gets its own colour. DOWN events are marked with a filled dot.
 *
 * Output size: [THUMB_W] × [THUMB_H] px, [Bitmap.Config.ARGB_8888].
 */
object ThumbnailGenerator {

    const val THUMB_W = 180
    const val THUMB_H = 320

    private val POINTER_COLORS = intArrayOf(
        Color.parseColor("#80B0FF"),  // blue
        Color.parseColor("#FF80B0"),  // pink
        Color.parseColor("#80FFB0"),  // green
        Color.parseColor("#FFB080"),  // orange
        Color.parseColor("#D080FF"),  // purple
    )

    fun generate(events: List<TouchEvent>): Bitmap {
        val bmp = Bitmap.createBitmap(THUMB_W, THUMB_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        if (events.size < 2) {
            if (events.isNotEmpty()) {
                canvas.drawCircle(
                    THUMB_W / 2f, THUMB_H / 2f, 10f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = POINTER_COLORS[0]
                        style = Paint.Style.FILL
                    }
                )
            }
            return bmp
        }

        // Bounding box of all recorded touch points
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (e in events) {
            if (e.x < minX) minX = e.x; if (e.x > maxX) maxX = e.x
            if (e.y < minY) minY = e.y; if (e.y > maxY) maxY = e.y
        }
        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val rangeY = (maxY - minY).coerceAtLeast(1f)

        // Uniform scale to fill 76 % of the canvas (12 % padding each side)
        val pad = 0.12f
        val scale = minOf(
            THUMB_W * (1f - 2 * pad) / rangeX,
            THUMB_H * (1f - 2 * pad) / rangeY
        )
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        fun tx(x: Float) = THUMB_W / 2f + (x - cx) * scale
        fun ty(y: Float) = THUMB_H / 2f + (y - cy) * scale

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        events.groupBy { it.pointerId }
            .values
            .forEachIndexed { idx, evts ->
                val base = POINTER_COLORS[idx % POINTER_COLORS.size]
                // 80 % opacity stroke, full-opacity dots
                strokePaint.color = (0xCC shl 24) or (base and 0x00FFFFFF)
                dotPaint.color = base

                val path = Path()
                var started = false
                for (e in evts) {
                    val px = tx(e.x); val py = ty(e.y)
                    when (e.action) {
                        TouchAction.DOWN -> {
                            path.moveTo(px, py)
                            canvas.drawCircle(px, py, 5f, dotPaint)
                            started = true
                        }
                        TouchAction.MOVE ->
                            if (started) path.lineTo(px, py)
                        TouchAction.UP, TouchAction.CANCEL ->
                            if (started) { path.lineTo(px, py); started = false }
                    }
                }
                canvas.drawPath(path, strokePaint)
            }

        return bmp
    }
}
