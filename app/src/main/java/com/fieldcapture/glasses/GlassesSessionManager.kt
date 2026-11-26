/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.glasses

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.Stream
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DatResult
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionError
import com.meta.wearable.dat.core.types.PermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlassesSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraStreamManager: CameraStreamManager
) {
    private val _state = MutableStateFlow<GlassesState>(GlassesState.Disconnected)
    val state: StateFlow<GlassesState> = _state.asStateFlow()

    private val _streamState = MutableStateFlow<StreamState?>(null)
    val streamState: StateFlow<StreamState?> = _streamState.asStateFlow()

    private val _frames = MutableSharedFlow<Bitmap>(extraBufferCapacity = 1)
    val frames: SharedFlow<Bitmap> = _frames.asSharedFlow()

    private var currentSession: DeviceSession? = null
    private var currentCamera: Camera? = null
    private var currentStream: Stream? = null

    private var sessionJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var isInitialized = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val listeners = mutableSetOf<GlassesEventListener>()

    fun registerListener(listener: GlassesEventListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: GlassesEventListener) {
        listeners.remove(listener)
    }

    /**
     * Initialize Meta DAT SDK once from Application.onCreate().
     */
    fun initialize() {
        if (isInitialized) return
        Wearables.initialize(context)
            .onSuccess {
                isInitialized = true
                Log.d(TAG, "Wearables SDK initialized")
            }
            .onFailure { error, throwable ->
                val msg = "SDK init failed: ${error.description}"
                Log.e(TAG, msg, throwable)
                updateState(GlassesState.Error(msg))
            }
    }

    suspend fun checkCameraPermission(): DatResult<PermissionStatus, PermissionError> {
        return Wearables.checkPermissionStatus(Permission.CAMERA)
    }

    /**
     * Start a new DAT DeviceSession with AutoDeviceSelector.
     */
    suspend fun startSession() {
        if (!isInitialized) {
            initialize()
        }
        if (currentSession != null) {
            Log.d(TAG, "Session already active, skipping startSession()")
            return
        }

        reconnectJob?.cancel()
        sessionJob?.cancel()
        updateState(GlassesState.Connecting)

        val session = Wearables.createSession(AutoDeviceSelector()).getOrElse { error ->
            val msg = "Create session failed: ${error.message ?: "unknown error"}"
            Log.e(TAG, msg, error)
            updateState(GlassesState.Error(msg))
            return
        }
        currentSession = session

        // Subscribe to state and errors BEFORE calling session.start()
        sessionJob = scope.launch {
            launch { observeSessionState(session) }
            launch { observeSessionErrors(session) }
        }

        Log.d(TAG, "Starting DeviceSession")
        session.start()
    }

    private suspend fun observeSessionState(session: DeviceSession) {
        session.state.collect { sessionState ->
            Log.d(TAG, "Session state -> $sessionState")
            when (sessionState) {
                DeviceSessionState.IDLE -> updateState(GlassesState.Disconnected)
                DeviceSessionState.STARTING -> updateState(GlassesState.Connecting)
                DeviceSessionState.STARTED -> {
                    reconnectAttempts = 0
                    updateState(GlassesState.Connected)
                    attachCamera(session)
                }
                DeviceSessionState.PAUSED -> {
                    cameraStreamManager.setStreamActive(false)
                    updateState(GlassesState.Paused)
                }
                DeviceSessionState.STOPPING -> {
                    Log.d(TAG, "Session stopping...")
                }
                DeviceSessionState.STOPPED -> {
                    Log.w(TAG, "Session reached terminal STOPPED state")
                    clearSessionResources()
                    updateState(GlassesState.Disconnected)
                    attemptReconnect()
                }
            }
        }
    }

    private suspend fun observeSessionErrors(session: DeviceSession) {
        session.errors.collect { error ->
            val msg = error.getLocalizedDescription(context)
            Log.e(TAG, "Session error: ${error.name} - $msg")
            updateState(GlassesState.Error(msg))
            listeners.forEach { it.onError(msg, null) }
        }
    }

    private suspend fun attachCamera(session: DeviceSession) {
        if (currentStream != null) return

        val camera = session.addCamera(
            StreamConfiguration(
                videoQuality = VideoQuality.MEDIUM,
                frameRate = 15,
                compressVideo = false
            )
        ).getOrElse { error ->
            val msg = "Camera attach failed: ${error.message ?: "unknown error"}"
            Log.e(TAG, msg, error)
            updateState(GlassesState.Error(msg))
            return
        }

        currentCamera = camera
        val stream = camera.stream
        currentStream = stream

        scope.launch {
            stream.state.collect { state ->
                Log.d(TAG, "Stream state -> $state")
                _streamState.value = state
                when (state) {
                    StreamState.STREAMING -> cameraStreamManager.setStreamActive(true)
                    StreamState.PAUSED -> cameraStreamManager.setStreamActive(false)
                    StreamState.STOPPED, StreamState.CLOSED -> {
                        cameraStreamManager.setStreamActive(false)
                        currentStream = null
                    }
                    else -> Unit
                }
            }
        }

        scope.launch {
            stream.errorStream.collect { streamError ->
                val msg = "Stream error: ${streamError.getLocalizedDescription(context)}"
                Log.e(TAG, msg)
                updateState(GlassesState.Error(msg))
            }
        }

        scope.launch {
            stream.videoStream.collect { frame ->
                val bitmap = frameToBitmap(frame) ?: return@collect
                cameraStreamManager.updateFrame(bitmap)
                _frames.tryEmit(bitmap)
                listeners.forEach { it.onFrameAvailable(bitmap) }
            }
        }

        stream.start().onFailure { error, throwable ->
            val msg = "Stream start failed: ${error.description}"
            Log.e(TAG, msg, throwable)
            updateState(GlassesState.Error(msg))
        }
    }

    suspend fun capturePhoto(): Result<ByteArray> {
        val stream = currentStream ?: return Result.failure(IllegalStateException("No active stream"))
        return stream.capturePhoto().fold(
            onSuccess = { photoData ->
                when (photoData) {
                    is PhotoData.HEIC -> Result.success(photoData.data.toByteArray())
                    is PhotoData.Bitmap -> {
                        val out = ByteArrayOutputStream()
                        photoData.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        Result.success(out.toByteArray())
                    }
                    else -> Result.failure(IllegalStateException("Unsupported PhotoData type"))
                }
            },
            onFailure = { error, throwable ->
                Result.failure(throwable ?: Exception(error.description))
            }
        )
    }

    private fun attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            val msg = "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached"
            Log.w(TAG, msg)
            updateState(GlassesState.Error(msg))
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS in ${RECONNECT_DELAY_MS}ms")
            delay(RECONNECT_DELAY_MS)

            val available = Wearables.devices.first()
            if (available.isNotEmpty()) {
                startSession()
            } else {
                Log.d(TAG, "No devices currently visible; observing Wearables.devices")
                Wearables.devices.first { it.isNotEmpty() }
                startSession()
            }
        }
    }

    fun pauseSession() {
        // Device-initiated pauses (e.g., folding glasses) arrive via DeviceSessionState.PAUSED
        Log.d(TAG, "pauseSession() tracked (device-driven in DAT SDK)")
    }

    fun stopSession() {
        Log.d(TAG, "Stopping stream and session")
        currentStream?.stop()
        currentCamera?.stop()
        currentSession?.stop()
    }

    fun cleanup() {
        reconnectJob?.cancel()
        sessionJob?.cancel()
        stopSession()
        clearSessionResources()
        scope.cancel()
    }

    private fun clearSessionResources() {
        cameraStreamManager.setStreamActive(false)
        _streamState.value = null
        currentStream = null
        currentCamera = null
        currentSession = null
    }

    private fun updateState(newState: GlassesState) {
        _state.value = newState
        listeners.forEach { it.onStateChanged(newState) }
    }

    private fun frameToBitmap(frame: VideoFrame): Bitmap? {
        if (frame.isCodecConfig) return null
        val bytes = frame.buffer.toByteArray()
        if (bytes.isEmpty()) return null

        if (frame.isCompressed) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        // Uncompressed DAT VideoFrame is YUV420 (I420); convert I420 -> NV21 for YuvImage
        val ySize = frame.width * frame.height
        val uvSize = ySize / 4
        if (bytes.size < ySize + uvSize * 2) return null

        val nv21 = ByteArray(ySize + uvSize * 2)
        System.arraycopy(bytes, 0, nv21, 0, ySize)
        var uvIndex = ySize
        for (i in 0 until uvSize) {
            nv21[uvIndex++] = bytes[ySize + uvSize + i] // V
            nv21[uvIndex++] = bytes[ySize + i]          // U
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, frame.width, frame.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, frame.width, frame.height), 85, out)
        val jpegBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val dup = duplicate()
        dup.rewind()
        return ByteArray(dup.remaining()).also { dup.get(it) }
    }

    companion object {
        private const val TAG = "GlassesSessionManager"
        private const val RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
