package com.macrorecorder.app.data.mapper

import com.google.gson.Gson
import com.macrorecorder.app.data.local.db.entity.MacroEntity
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MacroMapperTest {

    private val gson = Gson()

    private val fullSettings = MacroSettings(
        repeatCount            = 5,
        speed                  = 2.0f,
        pauseBetweenRunsMs     = 3_000L,
        scheduledTimeMs        = 1_750_000_000_000L,
        intervalMinutes        = 30,
        selectedDays           = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
        emergencyStopEnabled   = false,
        vibrationEnabled       = true,
        visualIndicatorEnabled = true
    )

    private val sampleMacro = Macro(
        id            = "test-uuid-abcd",
        name          = "Full Test Macro",
        createdAt     = 1_700_000_000_000L,
        duration      = 8_500L,
        eventCount    = 99,
        thumbnailPath = null,
        settings      = fullSettings
    )

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    fun `toEntity preserves id, name, timestamps and counts`() {
        val entity = sampleMacro.toEntity(gson)
        assertEquals(sampleMacro.id, entity.id)
        assertEquals(sampleMacro.name, entity.name)
        assertEquals(sampleMacro.createdAt, entity.createdAt)
        assertEquals(sampleMacro.duration, entity.duration)
        assertEquals(sampleMacro.eventCount, entity.eventCount)
    }

    @Test
    fun `toEntity serialises settingsJson as non-blank JSON`() {
        val entity = sampleMacro.toEntity(gson)
        assertTrue(entity.settingsJson.isNotBlank())
        assertTrue(entity.settingsJson.startsWith("{"))
    }

    @Test
    fun `toEntity settingsJson contains speed field`() {
        val entity = sampleMacro.toEntity(gson)
        assertTrue(entity.settingsJson.contains("speed"))
    }

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Test
    fun `toDomain restores all scalar Macro fields`() {
        val restored = sampleMacro.toEntity(gson).toDomain(gson)
        assertEquals(sampleMacro.id, restored.id)
        assertEquals(sampleMacro.name, restored.name)
        assertEquals(sampleMacro.createdAt, restored.createdAt)
        assertEquals(sampleMacro.duration, restored.duration)
        assertEquals(sampleMacro.eventCount, restored.eventCount)
    }

    @Test
    fun `toDomain restores MacroSettings repeatCount and speed`() {
        val s = sampleMacro.toEntity(gson).toDomain(gson).settings
        assertEquals(fullSettings.repeatCount, s.repeatCount)
        assertEquals(fullSettings.speed, s.speed, 0.001f)
    }

    @Test
    fun `toDomain restores pauseBetweenRunsMs`() {
        val s = sampleMacro.toEntity(gson).toDomain(gson).settings
        assertEquals(fullSettings.pauseBetweenRunsMs, s.pauseBetweenRunsMs)
    }

    @Test
    fun `toDomain restores scheduledTimeMs`() {
        val s = sampleMacro.toEntity(gson).toDomain(gson).settings
        assertEquals(fullSettings.scheduledTimeMs, s.scheduledTimeMs)
    }

    @Test
    fun `toDomain restores intervalMinutes`() {
        val s = sampleMacro.toEntity(gson).toDomain(gson).settings
        assertEquals(fullSettings.intervalMinutes, s.intervalMinutes)
    }

    @Test
    fun `toDomain restores selectedDays list`() {
        val s = sampleMacro.toEntity(gson).toDomain(gson).settings
        assertEquals(fullSettings.selectedDays, s.selectedDays)
    }

    @Test
    fun `toDomain restores boolean toggles`() {
        val s = sampleMacro.toEntity(gson).toDomain(gson).settings
        assertEquals(fullSettings.emergencyStopEnabled, s.emergencyStopEnabled)
        assertEquals(fullSettings.vibrationEnabled, s.vibrationEnabled)
        assertEquals(fullSettings.visualIndicatorEnabled, s.visualIndicatorEnabled)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `malformed settingsJson falls back to default MacroSettings`() {
        val entity = MacroEntity(
            id            = "id",
            name          = "name",
            createdAt     = 0L,
            duration      = 0L,
            eventCount    = 0,
            thumbnailPath = null,
            settingsJson  = "NOT_VALID_JSON{{{"
        )
        assertEquals(MacroSettings(), entity.toDomain(gson).settings)
    }

    @Test
    fun `empty settingsJson falls back to default MacroSettings`() {
        val entity = MacroEntity(
            id            = "id",
            name          = "name",
            createdAt     = 0L,
            duration      = 0L,
            eventCount    = 0,
            thumbnailPath = null,
            settingsJson  = ""
        )
        assertEquals(MacroSettings(), entity.toDomain(gson).settings)
    }

    @Test
    fun `null thumbnailPath round-trips as null`() {
        val restored = sampleMacro.copy(thumbnailPath = null).toEntity(gson).toDomain(gson)
        assertNull(restored.thumbnailPath)
    }

    @Test
    fun `non-null thumbnailPath round-trips correctly`() {
        val path = "/data/user/0/com.macrorecorder.app/thumb.png"
        val restored = sampleMacro.copy(thumbnailPath = path).toEntity(gson).toDomain(gson)
        assertEquals(path, restored.thumbnailPath)
    }

    @Test
    fun `infinite repeatCount (-1) round-trips correctly`() {
        val macro = sampleMacro.copy(settings = fullSettings.copy(repeatCount = -1))
        assertEquals(-1, macro.toEntity(gson).toDomain(gson).settings.repeatCount)
    }

    @Test
    fun `null scheduledTimeMs round-trips as null`() {
        val macro = sampleMacro.copy(settings = fullSettings.copy(scheduledTimeMs = null))
        assertNull(macro.toEntity(gson).toDomain(gson).settings.scheduledTimeMs)
    }

    @Test
    fun `null intervalMinutes round-trips as null`() {
        val macro = sampleMacro.copy(settings = fullSettings.copy(intervalMinutes = null))
        assertNull(macro.toEntity(gson).toDomain(gson).settings.intervalMinutes)
    }

    @Test
    fun `empty selectedDays round-trips as empty list`() {
        val macro = sampleMacro.copy(settings = fullSettings.copy(selectedDays = emptyList()))
        assertTrue(macro.toEntity(gson).toDomain(gson).settings.selectedDays.isEmpty())
    }
}
