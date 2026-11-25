package com.fieldcapture.domain.model

sealed class VoiceCommand {
    data object CaptureNameplate : VoiceCommand()
    data class CaptureDamage(val room: String, val surface: String) : VoiceCommand()
    data class Note(val text: String) : VoiceCommand()
    data class Unknown(val rawText: String) : VoiceCommand()
}
