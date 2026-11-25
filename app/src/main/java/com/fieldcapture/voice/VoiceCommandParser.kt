package com.fieldcapture.voice

import com.fieldcapture.domain.model.VoiceCommand
import javax.inject.Inject

class VoiceCommandParser @Inject constructor() {

    fun parse(rawText: String): VoiceCommand {
        val normalized = rawText.trim().lowercase()
        if (normalized.isBlank()) return VoiceCommand.Unknown(rawText)

        // TODO: Refine intent rules / regex for nameplate, damage (room + surface), and note commands
        return when {
            normalized.contains("nameplate") -> VoiceCommand.CaptureNameplate
            normalized.startsWith("note ") || normalized.startsWith("note:") -> {
                val noteBody = rawText.trim().removePrefix("note").removePrefix("Note").removePrefix(":").trim()
                VoiceCommand.Note(noteBody)
            }
            normalized.contains("damage") || normalized.contains("capture") -> {
                // TODO: Extract room and surface entities from spoken phrase
                VoiceCommand.CaptureDamage(room = "unknown", surface = "unknown")
            }
            else -> VoiceCommand.Unknown(rawText)
        }
    }
}
