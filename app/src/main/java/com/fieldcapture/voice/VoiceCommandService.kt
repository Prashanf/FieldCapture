package com.fieldcapture.voice

import android.content.Context
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone-mic speech recognition service (fallback when Mock Device Kit does not stream audio).
 */
@Singleton
class VoiceCommandService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: VoiceCommandParser
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: VoiceCommandCallback? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun setCallback(callback: VoiceCommandCallback?) {
        this.callback = callback
    }

    fun startListening() {
        // TODO: Initialize Android SpeechRecognizer on main thread, configure RecognizerIntent,
        //       handle RecognitionListener callbacks, and auto-restart for continuous hands-free listening
        _isListening.value = true
        callback?.onListeningStateChanged(true)
    }

    fun stopListening() {
        // TODO: Stop and destroy SpeechRecognizer cleanly
        speechRecognizer?.stopListening()
        _isListening.value = false
        callback?.onListeningStateChanged(false)
    }

    fun simulateSpokenText(rawText: String) {
        val command = parser.parse(rawText)
        callback?.onCommandRecognized(command)
    }
}
