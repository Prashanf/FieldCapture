package com.fieldcapture.voice

import com.fieldcapture.domain.model.VoiceCommand

interface VoiceCommandCallback {
    fun onCommandRecognized(command: VoiceCommand)
    fun onPartialText(text: String)
    fun onListeningStateChanged(isListening: Boolean)
    fun onError(errorCode: Int, message: String)
}
