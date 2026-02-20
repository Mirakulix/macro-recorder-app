package com.macrorecorder.app.domain.model

/**
 * A single captured touch interaction.
 *
 * @param timestampMs Milliseconds since the first event of this recording (relative, starts at 0).
 * @param x           X coordinate in screen pixels.
 * @param y           Y coordinate in screen pixels.
 * @param action      Type of touch event.
 * @param pressure    Normalised pressure [0.0, 1.0]; defaults to 1.0 when device doesn't report it.
 * @param pointerId   Pointer index for multi-touch sequences; 0 for single-touch.
 */
data class TouchEvent(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val action: TouchAction,
    val pressure: Float = 1.0f,
    val pointerId: Int = 0
)
