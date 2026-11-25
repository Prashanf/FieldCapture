package com.fieldcapture.voice

import com.fieldcapture.domain.model.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()

    // Nameplate variations
    @Test
    fun `parse capture nameplate`() {
        assertTrue(parser.parse("capture nameplate") is VoiceCommand.CaptureNameplate)
    }

    @Test
    fun `parse capture the nameplate`() {
        assertTrue(parser.parse("capture the nameplate") is VoiceCommand.CaptureNameplate)
    }

    @Test
    fun `parse capture name plate with space`() {
        assertTrue(parser.parse("capture name plate") is VoiceCommand.CaptureNameplate)
    }

    @Test
    fun `parse plural captures nameplate`() {
        assertTrue(parser.parse("captures nameplate") is VoiceCommand.CaptureNameplate)
    }

    // Damage variations
    @Test
    fun `parse capture damage with room and surface`() {
        val cmd = parser.parse("capture damage kitchen north wall") as VoiceCommand.CaptureDamage
        assertEquals("kitchen", cmd.room)
        assertEquals("north wall", cmd.surface)
    }

    @Test
    fun `parse capture damage with room only`() {
        val cmd = parser.parse("capture damage bathroom") as VoiceCommand.CaptureDamage
        assertEquals("bathroom", cmd.room)
        assertEquals("unknown", cmd.surface)
    }

    @Test
    fun `parse capture damage with commas`() {
        val cmd = parser.parse("capture damage, kitchen, north wall") as VoiceCommand.CaptureDamage
        assertEquals("kitchen", cmd.room)
        assertEquals("north wall", cmd.surface)
    }

    @Test
    fun `parse capture damage ceiling`() {
        val cmd = parser.parse("capture damage bedroom ceiling") as VoiceCommand.CaptureDamage
        assertEquals("bedroom", cmd.room)
        assertEquals("ceiling", cmd.surface)
    }

    @Test
    fun `parse capture damage with no room or surface`() {
        val cmd = parser.parse("capture damage") as VoiceCommand.CaptureDamage
        assertEquals("unknown", cmd.room)
        assertEquals("unknown", cmd.surface)
    }

    @Test
    fun `parse capture damage surface only`() {
        val cmd = parser.parse("capture damage north wall") as VoiceCommand.CaptureDamage
        assertEquals("unknown", cmd.room)
        assertEquals("north wall", cmd.surface)
    }

    // Note variations
    @Test
    fun `parse note with colon`() {
        val cmd = parser.parse("note: subfloor soft by the cabinet") as VoiceCommand.Note
        assertEquals("subfloor soft by the cabinet", cmd.text)
    }

    @Test
    fun `parse note without colon`() {
        val cmd = parser.parse("note subfloor is soft") as VoiceCommand.Note
        assertEquals("subfloor is soft", cmd.text)
    }

    @Test
    fun `parse add note variation`() {
        val cmd = parser.parse("add note water stain visible") as VoiceCommand.Note
        assertEquals("water stain visible", cmd.text)
    }

    @Test
    fun `parse take note variation`() {
        val cmd = parser.parse("take note shutoff valve behind dryer") as VoiceCommand.Note
        assertEquals("shutoff valve behind dryer", cmd.text)
    }

    @Test
    fun `parse empty note as unknown`() {
        assertTrue(parser.parse("note:") is VoiceCommand.Unknown)
    }

    // Unknown
    @Test
    fun `parse random speech as unknown`() {
        assertTrue(parser.parse("hello world") is VoiceCommand.Unknown)
    }

    @Test
    fun `parse empty string as unknown`() {
        assertTrue(parser.parse("") is VoiceCommand.Unknown)
    }
}
