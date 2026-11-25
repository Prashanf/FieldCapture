package com.fieldcapture.voice

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    }.getOrNull()

    fun initialize() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.2f)
                isTtsReady = true
            } else {
                Log.w(TAG, "TextToSpeech init failed with status=$status")
            }
        }
    }

    fun confirmCapture(type: String) {
        playTone()
        speak("$type captured")
    }

    fun requestRetake(reason: String) {
        speak("Please retake. Image is $reason")
    }

    fun noteRecorded() {
        playTone()
        speak("Note recorded")
    }

    fun commandNotRecognized() {
        speak("Command not recognized")
    }

    fun sessionConnected() {
        speak("Glasses connected. Ready to capture.")
    }

    fun sessionDisconnected() {
        speak("Connection lost. Reconnecting.")
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
        }
    }

    private fun playTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        isTtsReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        runCatching { toneGenerator?.release() }
    }

    companion object {
        private const val TAG = "AudioFeedbackManager"
    }
}
