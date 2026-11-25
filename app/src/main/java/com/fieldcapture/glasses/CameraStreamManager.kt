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

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun setStreamActive(active: Boolean) {
        _isActive.value = active
        if (!active) {
            _latestFrame.value = null
        }
    }

    fun updateFrame(bitmap: Bitmap) {
        _latestFrame.value = bitmap
    }

    fun captureSnapshot(): Bitmap? = _latestFrame.value
}
