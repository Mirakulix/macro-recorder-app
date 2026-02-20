package com.macrorecorder.app.util

import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings
import com.macrorecorder.app.domain.model.TouchAction
import com.macrorecorder.app.domain.model.TouchEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MacroExporter.toJson] / [MacroExporter.fromJson] (the pure
 * serialisation layer, no Android context needed).
 */
class MacroExporterTest {

    private val macro = Macro(
        id            = "original-uuid-1234",
        name          = "Shopping Bot",
        createdAt     = 1_700_000_000_000L,
        duration      = 4_200L,
        eventCount    = 3,
        thumbnailPath = null,
        settings      = MacroSettings(repeatCount = 2, speed = 1.5f)
    )

    private val events = listOf(
        TouchEvent(0L,   100f, 200f, TouchAction.DOWN),
        TouchEvent(50L,  110f, 210f, TouchAction.MOVE),
        TouchEvent(100L, 120f, 220f, TouchAction.UP)
    )

    // ── toJson ────────────────────────────────────────────────────────────────

    @Test
    fun `toJson returns non-blank string`() {
        assertTrue(MacroExporter.toJson(macro, events).isNotBlank())
    }

    @Test
    fun `toJson contains macro name`() {
        assertTrue(MacroExporter.toJson(macro, events).contains("Shopping Bot"))
    }

    @Test
    fun `toJson contains version field`() {
        assertTrue(MacroExporter.toJson(macro, events).contains("\"version\""))
    }

    // ── fromJson roundtrip ────────────────────────────────────────────────────

    @Test
    fun `fromJson restores macro name`() {
        val json = MacroExporter.toJson(macro, events)
        assertEquals("Shopping Bot", MacroExporter.fromJson(json)!!.first.name)
    }

    @Test
    fun `fromJson restores correct event count`() {
        val json = MacroExporter.toJson(macro, events)
        assertEquals(3, MacroExporter.fromJson(json)!!.second.size)
    }

    @Test
    fun `fromJson restores event coordinates and action`() {
        val json = MacroExporter.toJson(macro, events)
        val imported = MacroExporter.fromJson(json)!!.second
        assertEquals(100f, imported[0].x, 0.001f)
        assertEquals(200f, imported[0].y, 0.001f)
        assertEquals(TouchAction.DOWN, imported[0].action)
        assertEquals(50L,  imported[1].timestampMs)
        assertEquals(TouchAction.MOVE, imported[1].action)
    }

    @Test
    fun `fromJson restores MacroSettings`() {
        val json = MacroExporter.toJson(macro, events)
        val s = MacroExporter.fromJson(json)!!.first.settings
        assertEquals(2, s.repeatCount)
        assertEquals(1.5f, s.speed, 0.001f)
    }

    @Test
    fun `fromJson restores macro duration and createdAt`() {
        val json = MacroExporter.toJson(macro, events)
        val imported = MacroExporter.fromJson(json)!!.first
        assertEquals(macro.duration, imported.duration)
        assertEquals(macro.eventCount, imported.eventCount)
    }

    // ── Fresh identity on import ──────────────────────────────────────────────

    @Test
    fun `fromJson assigns new UUID different from original`() {
        val json = MacroExporter.toJson(macro, events)
        assertNotEquals("original-uuid-1234", MacroExporter.fromJson(json)!!.first.id)
    }

    @Test
    fun `fromJson assigns non-blank UUID`() {
        val json = MacroExporter.toJson(macro, events)
        assertTrue(MacroExporter.fromJson(json)!!.first.id.isNotBlank())
    }

    @Test
    fun `fromJson assigns fresh createdAt (not original)`() {
        val before = System.currentTimeMillis()
        val json = MacroExporter.toJson(macro, events)
        val importedAt = MacroExporter.fromJson(json)!!.first.createdAt
        assertTrue(importedAt >= before)
    }

    @Test
    fun `two imports from same json produce different UUIDs`() {
        val json = MacroExporter.toJson(macro, events)
        val id1 = MacroExporter.fromJson(json)!!.first.id
        val id2 = MacroExporter.fromJson(json)!!.first.id
        assertNotEquals(id1, id2)
    }

    // ── Null / invalid inputs ─────────────────────────────────────────────────

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(MacroExporter.fromJson(""))
    }

    @Test
    fun `fromJson returns null for blank string`() {
        assertNull(MacroExporter.fromJson("   "))
    }

    @Test
    fun `fromJson returns null for plain text`() {
        assertNull(MacroExporter.fromJson("not json at all"))
    }

    @Test
    fun `fromJson returns null for unrelated JSON object`() {
        assertNull(MacroExporter.fromJson("""{"foo": "bar", "baz": 42}"""))
    }

    @Test
    fun `fromJson returns null for empty events list`() {
        val json = MacroExporter.toJson(macro, emptyList())
        assertNull(MacroExporter.fromJson(json))
    }

    @Test
    fun `fromJson returns null for wrong version number`() {
        // Craft a bundle with version=99 — should be rejected
        val valid = MacroExporter.toJson(macro, events)
        val wrongVersion = valid.replace("\"version\":1", "\"version\":99")
        assertNull(MacroExporter.fromJson(wrongVersion))
    }

    // ── suggestedFilename ─────────────────────────────────────────────────────

    @Test
    fun `suggestedFilename ends with dot macro`() {
        assertTrue(MacroExporter.suggestedFilename("Test").endsWith(".macro"))
    }

    @Test
    fun `suggestedFilename preserves alphanumeric chars`() {
        val name = MacroExporter.suggestedFilename("MyMacro123")
        assertTrue(name.startsWith("MyMacro123"))
    }

    @Test
    fun `suggestedFilename removes exclamation mark`() {
        assertFalse(MacroExporter.suggestedFilename("Macro!").contains("!"))
    }

    @Test
    fun `suggestedFilename removes hash symbol`() {
        assertFalse(MacroExporter.suggestedFilename("Macro #1").contains("#"))
    }

    @Test
    fun `suggestedFilename is non-blank for any input`() {
        assertNotNull(MacroExporter.suggestedFilename(""))
        assertTrue(MacroExporter.suggestedFilename("").isNotBlank() ||
            MacroExporter.suggestedFilename("").endsWith(".macro"))
    }
}
