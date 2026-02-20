package com.macrorecorder.app.service.execution

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests for [ScheduledMacroReceiver.isTodayActive] — the day-of-week filter
 * that decides whether a scheduled macro should run today.
 *
 * Calendar day constants: Sunday=1, Monday=2, …, Saturday=7.
 */
class ScheduledMacroReceiverTest {

    private val receiver = ScheduledMacroReceiver()

    private val today: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    /** Returns a Calendar day constant that is guaranteed to not be today. */
    private fun notToday(): Int = if (today == Calendar.SUNDAY) Calendar.MONDAY
                                  else Calendar.SUNDAY

    /** Returns all Calendar day constants (1–7) except today. */
    private fun allDaysExceptToday(): List<Int> = (1..7).filter { it != today }

    // ── Empty list = every day ────────────────────────────────────────────────

    @Test
    fun `empty selectedDays is always active`() {
        assertTrue(receiver.isTodayActive(emptyList()))
    }

    // ── Matching today ────────────────────────────────────────────────────────

    @Test
    fun `list containing only today is active`() {
        assertTrue(receiver.isTodayActive(listOf(today)))
    }

    @Test
    fun `list containing today among other days is active`() {
        val days = listOf(notToday(), today)
        assertTrue(receiver.isTodayActive(days))
    }

    @Test
    fun `full week list is always active`() {
        assertTrue(receiver.isTodayActive((1..7).toList()))
    }

    // ── Not matching today ────────────────────────────────────────────────────

    @Test
    fun `list with single day that is not today is inactive`() {
        assertFalse(receiver.isTodayActive(listOf(notToday())))
    }

    @Test
    fun `list of all days except today is inactive`() {
        val others = allDaysExceptToday()
        // Guard: if somehow the list includes today the test would be vacuous.
        assert(today !in others)
        assertFalse(receiver.isTodayActive(others))
    }

    @Test
    fun `two days not including today is inactive`() {
        val others = allDaysExceptToday().take(2)
        assertFalse(receiver.isTodayActive(others))
    }

    // ── Boundary: single-element list ─────────────────────────────────────────

    @Test
    fun `each individual non-today day is inactive`() {
        allDaysExceptToday().forEach { day ->
            assertFalse("Day $day should be inactive but was active",
                receiver.isTodayActive(listOf(day)))
        }
    }
}
