package com.fieldcapture.glasses

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlassesSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mockDeviceManager: MockDeviceManager,
    private val cameraStreamManager: CameraStreamManager
) {
    private val _state = MutableStateFlow<GlassesState>(GlassesState.Disconnected)
    val state: StateFlow<GlassesState> = _state.asStateFlow()

    private val listeners = mutableSetOf<GlassesEventListener>()

    fun registerListener(listener: GlassesEventListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: GlassesEventListener) {
        listeners.remove(listener)
    }

    fun initializeSdk(): Result<Unit> = runCatching {
        // TODO: Initialize Meta Wearables DAT SDK (mwdat-core / mwdat-device / mwdat-state)
        //       and register app with MockDeviceManager in mock mode
        mockDeviceManager.setupMockDevice().getOrThrow()
    }

    fun connect(): Result<Unit> = runCatching {
        updateState(GlassesState.Connecting)
        // TODO: Start DAT session lifecycle and connect to Mock Ray-Ban Meta Gen 2 device
        cameraStreamManager.startStream().getOrThrow()
        updateState(GlassesState.Connected())
    }

    fun pause() {
        // TODO: Pause DAT stream session cleanly
        updateState(GlassesState.Paused)
    }

    fun disconnect() {
        cameraStreamManager.stopStream()
        // TODO: Tear down DAT session and release resources
        updateState(GlassesState.Disconnected)
    }

    private fun updateState(newState: GlassesState) {
        _state.value = newState
        listeners.forEach { it.onStateChanged(newState) }
    }
}
