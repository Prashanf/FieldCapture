package com.fieldcapture.voice

import com.fieldcapture.domain.model.VoiceCommand
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandParser @Inject constructor() {

    /**
     * Parses recognized speech text into a structured VoiceCommand.
     *
     * Handles variations like:
     * - "capture nameplate" / "capture the nameplate" / "capture name plate"
     * - "capture damage kitchen north wall" / "capture damage, kitchen, north wall"
     * - "note subfloor soft by the cabinet" / "note: subfloor soft by the cabinet"
     */
    fun parse(rawText: String): VoiceCommand {
        val text = rawText.lowercase().trim()
            .replace(",", " ")
            .replace(":", " ")
            .replace("\\s+".toRegex(), " ")

        return when {
            isNameplateCommand(text) -> VoiceCommand.CaptureNameplate
            isDamageCommand(text) -> parseDamageCommand(text)
            isNoteCommand(text) -> parseNoteCommand(text)
            else -> VoiceCommand.Unknown(rawText)
        }
    }

    private fun isNameplateCommand(text: String): Boolean {
        return text.matches(Regex(".*captures?\\s+(the\\s+)?name\\s*plate.*")) ||
            text == "capture nameplate" ||
            text == "nameplate"
    }

    private fun isDamageCommand(text: String): Boolean {
        return text.startsWith("capture damage") ||
            text.startsWith("capture the damage") ||
            text.startsWith("captures damage")
    }

    private fun parseDamageCommand(text: String): VoiceCommand {
        val remainder = text
            .replace(Regex("^captures?\\s+(the\\s+)?damage\\s*"), "")
            .trim()

        if (remainder.isBlank()) {
            return VoiceCommand.CaptureDamage(room = "unknown", surface = "unknown")
        }

        val words = remainder.split(" ").filter { it.isNotBlank() }

        val knownSurfaces = setOf(
            "wall", "ceiling", "floor", "baseboard", "trim",
            "cabinet", "door", "window", "pipe", "subfloor"
        )
        val knownDirections = setOf(
            "north", "south", "east", "west",
            "left", "right", "upper", "lower", "top", "bottom"
        )

        var surfaceStartIndex = words.size
        for (i in words.indices.reversed()) {
            if (words[i] in knownSurfaces || words[i] in knownDirections) {
                surfaceStartIndex = i
            } else {
                break
            }
        }

        return when {
            surfaceStartIndex == 0 -> {
                VoiceCommand.CaptureDamage(room = "unknown", surface = words.joinToString(" "))
            }
            surfaceStartIndex >= words.size -> {
                if (words.size > 1) {
                    VoiceCommand.CaptureDamage(
                        room = words.dropLast(1).joinToString(" "),
                        surface = words.last()
                    )
                } else {
                    VoiceCommand.CaptureDamage(room = words.first(), surface = "unknown")
                }
            }
            else -> {
                VoiceCommand.CaptureDamage(
                    room = words.take(surfaceStartIndex).joinToString(" "),
                    surface = words.drop(surfaceStartIndex).joinToString(" ")
                )
            }
        }
    }

    private fun isNoteCommand(text: String): Boolean {
        return text.startsWith("note ") ||
            text.startsWith("note:") ||
            text.startsWith("add note") ||
            text.startsWith("take note") ||
            text == "note" ||
            text == "add note" ||
            text == "take note"
    }

    private fun parseNoteCommand(text: String): VoiceCommand {
        val noteText = text
            .replace(Regex("^(add\\s+|take\\s+)?note\\s*:?\\s*"), "")
            .trim()

        return if (noteText.isBlank()) {
            VoiceCommand.Unknown(text)
        } else {
            VoiceCommand.Note(noteText)
        }
    }
}
