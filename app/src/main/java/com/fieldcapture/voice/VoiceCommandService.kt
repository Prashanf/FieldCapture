package com.fieldcapture.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Continuous speech recognition service using Android's native SpeechRecognizer.
 *
 * Phone mic fallback: Mock Device Kit simulates the glasses camera feed but does NOT provide
 * an audio/microphone path from the simulated device, so voice recognition uses the phone's
 * built-in microphone. On real glasses hardware, this would be replaced/augmented by DAT SDK's
 * microphone capability (MWDATMedia).
 */
@Singleton
class VoiceCommandService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldRestart = false

    private val _recognizedText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val recognizedText: SharedFlow<String> = _recognizedText.asSharedFlow()

    private val _listeningState = MutableStateFlow(ListeningState.IDLE)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    enum class ListeningState {
        IDLE,
        LISTENING,
        PROCESSING,
        ERROR
    }

    fun startListening() {
        runOnMainThread {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "SpeechRecognizer is not available on this device")
                _listeningState.value = ListeningState.ERROR
                return@runOnMainThread
            }

            mainHandler.removeCallbacksAndMessages(null)
            shouldRestart = true
            initializeRecognizer()
            startRecognition()
        }
    }

    fun stopListening() {
        runOnMainThread {
            shouldRestart = false
            isListening = false
            mainHandler.removeCallbacksAndMessages(null)
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
            _listeningState.value = ListeningState.IDLE
        }
    }

    private fun initializeRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
    }

    private fun startRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        try {
            isListening = true
            _listeningState.value = ListeningState.LISTENING
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            isListening = false
            _listeningState.value = ListeningState.ERROR
            restartListeningAfterDelay(delayMs = 1000L)
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _listeningState.value = ListeningState.LISTENING
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _listeningState.value = ListeningState.PROCESSING
        }

        override fun onError(error: Int) {
            isListening = false
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal silence timeout — restart immediately
                    restartListeningAfterDelay(delayMs = 100L)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    Log.d(TAG, "Recognizer busy; skipping restart")
                }
                else -> {
                    Log.w(TAG, "SpeechRecognizer error code=$error; retrying")
                    _listeningState.value = ListeningState.ERROR
                    restartListeningAfterDelay(delayMs = 1000L)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.lowercase()?.trim()

            if (!text.isNullOrBlank()) {
                Log.d(TAG, "Recognized speech: \"$text\"")
                _recognizedText.tryEmit(text)
            }

            restartListeningAfterDelay(delayMs = 100L)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Do not act on partial results to prevent false command triggers
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun restartListeningAfterDelay(delayMs: Long = 100L) {
        if (!shouldRestart) return
        mainHandler.postDelayed({
            if (shouldRestart) {
                initializeRecognizer()
                startRecognition()
            }
        }, delayMs)
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    companion object {
        private const val TAG = "VoiceCommandService"
    }
}
