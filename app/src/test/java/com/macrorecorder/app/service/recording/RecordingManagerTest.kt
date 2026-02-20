package com.macrorecorder.app.service.recording

import com.macrorecorder.app.domain.model.TouchAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordingManagerTest {

    @Before
    fun setUp() {
        // Always start from a clean, stopped state.
        if (RecordingManager.isRecording) RecordingManager.stop()
    }

    @After
    fun tearDown() {
        if (RecordingManager.isRecording) RecordingManager.stop()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `isRecording is false initially`() {
        assertFalse(RecordingManager.isRecording)
    }

    @Test
    fun `isPaused is false initially`() {
        assertFalse(RecordingManager.isPaused)
    }

    // ── start() ───────────────────────────────────────────────────────────────

    @Test
    fun `start sets isRecording true`() {
        RecordingManager.start()
        assertTrue(RecordingManager.isRecording)
    }

    @Test
    fun `start clears isPaused`() {
        RecordingManager.start()
        RecordingManager.pause()
        RecordingManager.stop()
        RecordingManager.start()
        assertFalse(RecordingManager.isPaused)
    }

    @Test
    fun `start clears lastResult`() {
        RecordingManager.start()
        RecordingManager.addEvent(1f, 2f, TouchAction.DOWN)
        RecordingManager.stop()
        assertNotNull(RecordingManager.lastResult)

        RecordingManager.start()
        assertNull(RecordingManager.lastResult)
    }

    @Test
    fun `start clears events from previous session`() {
        RecordingManager.start()
        RecordingManager.addEvent(1f, 2f, TouchAction.DOWN)
        RecordingManager.stop()

        RecordingManager.start()
        assertEquals(0, RecordingManager.eventCount)
    }

    // ── addEvent() ────────────────────────────────────────────────────────────

    @Test
    fun `addEvent before start is ignored`() {
        RecordingManager.addEvent(100f, 200f, TouchAction.DOWN)
        assertEquals(0, RecordingManager.eventCount)
    }

    @Test
    fun `addEvent while recording is captured`() {
        RecordingManager.start()
        RecordingManager.addEvent(100f, 200f, TouchAction.DOWN)
        RecordingManager.addEvent(110f, 210f, TouchAction.MOVE)
        RecordingManager.addEvent(120f, 220f, TouchAction.UP)
        assertEquals(3, RecordingManager.eventCount)
    }

    @Test
    fun `addEvent while paused is ignored`() {
        RecordingManager.start()
        RecordingManager.addEvent(10f, 10f, TouchAction.DOWN)
        RecordingManager.pause()
        RecordingManager.addEvent(20f, 20f, TouchAction.MOVE) // ignored
        RecordingManager.addEvent(30f, 30f, TouchAction.MOVE) // ignored
        RecordingManager.resume()
        RecordingManager.addEvent(40f, 40f, TouchAction.UP)
        assertEquals(2, RecordingManager.eventCount)
    }

    @Test
    fun `captured events have correct coordinates and action`() {
        RecordingManager.start()
        RecordingManager.addEvent(123f, 456f, TouchAction.DOWN, pressure = 0.8f, pointerId = 1)
        val result = RecordingManager.stop()
        val event = result.events.single()
        assertEquals(123f, event.x, 0.001f)
        assertEquals(456f, event.y, 0.001f)
        assertEquals(TouchAction.DOWN, event.action)
        assertEquals(0.8f, event.pressure, 0.001f)
        assertEquals(1, event.pointerId)
    }

    @Test
    fun `captured events have non-negative timestamps`() {
        RecordingManager.start()
        Thread.sleep(5)
        RecordingManager.addEvent(10f, 20f, TouchAction.DOWN)
        val result = RecordingManager.stop()
        assertTrue(result.events.first().timestampMs >= 0)
    }

    @Test
    fun `event timestamps are monotonically non-decreasing`() {
        RecordingManager.start()
        repeat(10) { RecordingManager.addEvent(it.toFloat(), 0f, TouchAction.MOVE) }
        val result = RecordingManager.stop()
        val timestamps = result.events.map { it.timestampMs }
        for (i in 1 until timestamps.size) {
            assertTrue(timestamps[i] >= timestamps[i - 1])
        }
    }

    // ── pause() / resume() ────────────────────────────────────────────────────

    @Test
    fun `pause sets isPaused`() {
        RecordingManager.start()
        RecordingManager.pause()
        assertTrue(RecordingManager.isPaused)
    }

    @Test
    fun `resume clears isPaused`() {
        RecordingManager.start()
        RecordingManager.pause()
        RecordingManager.resume()
        assertFalse(RecordingManager.isPaused)
    }

    // ── stop() ────────────────────────────────────────────────────────────────

    @Test
    fun `stop clears isRecording`() {
        RecordingManager.start()
        RecordingManager.stop()
        assertFalse(RecordingManager.isRecording)
    }

    @Test
    fun `stop clears isPaused`() {
        RecordingManager.start()
        RecordingManager.pause()
        RecordingManager.stop()
        assertFalse(RecordingManager.isPaused)
    }

    @Test
    fun `stop returns captured events`() {
        RecordingManager.start()
        RecordingManager.addEvent(1f, 2f, TouchAction.DOWN)
        RecordingManager.addEvent(3f, 4f, TouchAction.UP)
        val result = RecordingManager.stop()
        assertEquals(2, result.events.size)
    }

    @Test
    fun `stop stores result in lastResult`() {
        RecordingManager.start()
        RecordingManager.addEvent(50f, 60f, TouchAction.DOWN)
        val result = RecordingManager.stop()
        assertEquals(result, RecordingManager.lastResult)
    }

    @Test
    fun `stop without prior start returns empty result`() {
        val result = RecordingManager.stop()
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `duration is non-negative`() {
        RecordingManager.start()
        Thread.sleep(5)
        val result = RecordingManager.stop()
        assertTrue(result.durationMs >= 0)
    }

    // ── Multi-session ─────────────────────────────────────────────────────────

    @Test
    fun `second session is independent of first`() {
        RecordingManager.start()
        RecordingManager.addEvent(1f, 1f, TouchAction.DOWN)
        RecordingManager.stop()

        RecordingManager.start()
        RecordingManager.addEvent(2f, 2f, TouchAction.UP)
        val result = RecordingManager.stop()

        assertEquals(1, result.events.size)
        assertEquals(2f, result.events[0].x, 0.001f)
    }
}
