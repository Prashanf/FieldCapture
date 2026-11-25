package com.fieldcapture.voice

import com.fieldcapture.domain.model.VoiceCommand
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()

    @Test
    fun `parses nameplate command`() {
        val result = parser.parse("capture nameplate")
        assertTrue(result is VoiceCommand.CaptureNameplate)
    }

    @Test
    fun `parses note command`() {
        val result = parser.parse("note water shutoff valve is behind the dryer")
        assertTrue(result is VoiceCommand.Note)
    }

    @Test
    fun `returns unknown for unrecognized command`() {
        val result = parser.parse("hello glasses")
        assertTrue(result is VoiceCommand.Unknown)
    }

    // TODO: Add tests for room + surface extraction in CaptureDamage commands
}
