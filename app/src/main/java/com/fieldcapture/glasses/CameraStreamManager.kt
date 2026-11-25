package com.fieldcapture.glasses

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraStreamManager @Inject constructor() {

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private var isStreaming = false

    fun startStream(): Result<Unit> = runCatching {
        // TODO: Subscribe to Meta DAT SDK mwdat-camera stream / mock camera frames and emit to _latestFrame
        isStreaming = true
    }

    fun stopStream() {
        // TODO: Unsubscribe from DAT camera stream and release frame buffers
        isStreaming = false
    }

    fun captureSnapshot(): Bitmap? {
        // TODO: Trigger high-resolution photo capture via DAT camera API or return current frame
        return _latestFrame.value
    }

    fun updateFrame(bitmap: Bitmap) {
        _latestFrame.value = bitmap
    }

    fun isActive(): Boolean = isStreaming
}
