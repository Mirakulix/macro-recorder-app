package com.macrorecorder.app.service.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [MacroScheduler] request-code logic.
 *
 * These are pure JVM tests — no AlarmManager or Context is instantiated.
 * The goal is to verify that one-time and interval alarms for the same macro
 * always use distinct, stable, non-negative request codes, and that different
 * macros never share codes (within the test sample set).
 */
class MacroSchedulerTest {

    // ── Distinctness between once and interval ─────────────────────────────

    @Test
    fun `once and interval codes differ for same macroId`() {
        val id = "macro-aabb-ccdd"
        assertNotEquals(
            MacroScheduler.requestCodeOnce(id),
            MacroScheduler.requestCodeInterval(id)
        )
    }

    @Test
    fun `once and interval codes differ for empty string macroId`() {
        assertNotEquals(
            MacroScheduler.requestCodeOnce(""),
            MacroScheduler.requestCodeInterval("")
        )
    }

    // ── Stability (determinism) ────────────────────────────────────────────

    @Test
    fun `requestCodeOnce is deterministic`() {
        val id = "stable-id"
        assertEquals(
            MacroScheduler.requestCodeOnce(id),
            MacroScheduler.requestCodeOnce(id)
        )
    }

    @Test
    fun `requestCodeInterval is deterministic`() {
        val id = "stable-id"
        assertEquals(
            MacroScheduler.requestCodeInterval(id),
            MacroScheduler.requestCodeInterval(id)
        )
    }

    // ── Non-negative ──────────────────────────────────────────────────────

    @Test
    fun `requestCodeOnce is non-negative`() {
        listOf("", "a", "uuid-1234", "x".repeat(100)).forEach { id ->
            assertTrue("once code for '$id' is negative",
                MacroScheduler.requestCodeOnce(id) >= 0)
        }
    }

    @Test
    fun `requestCodeInterval is non-negative`() {
        listOf("", "a", "uuid-1234", "x".repeat(100)).forEach { id ->
            assertTrue("interval code for '$id' is negative",
                MacroScheduler.requestCodeInterval(id) >= 0)
        }
    }

    // ── Bit-layout invariant ──────────────────────────────────────────────

    @Test
    fun `once code never has bit 28 set`() {
        listOf("abc", "uuid-5678", "some-long-macro-id-here").forEach { id ->
            assertEquals("once code for '$id' unexpectedly has bit 28 set", 0,
                MacroScheduler.requestCodeOnce(id) and 0x10000000)
        }
    }

    @Test
    fun `interval code always has bit 28 set`() {
        listOf("abc", "uuid-5678", "some-long-macro-id-here").forEach { id ->
            assertNotEquals("interval code for '$id' is missing bit 28", 0,
                MacroScheduler.requestCodeInterval(id) and 0x10000000)
        }
    }

    // ── Cross-macro distinctness ──────────────────────────────────────────

    @Test
    fun `different macro ids produce different once codes`() {
        val ids = listOf("macro-a", "macro-b", "macro-c", "macro-d")
        val codes = ids.map { MacroScheduler.requestCodeOnce(it) }
        assertEquals("some once codes are duplicated", codes.size, codes.toSet().size)
    }

    @Test
    fun `different macro ids produce different interval codes`() {
        val ids = listOf("macro-a", "macro-b", "macro-c", "macro-d")
        val codes = ids.map { MacroScheduler.requestCodeInterval(it) }
        assertEquals("some interval codes are duplicated", codes.size, codes.toSet().size)
    }
}
