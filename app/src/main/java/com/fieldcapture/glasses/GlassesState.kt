package com.fieldcapture.glasses

sealed class GlassesState {
    data object Disconnected : GlassesState()
    data object Connecting : GlassesState()
    data class Connected(val deviceName: String = "mock_rayban_meta_gen2") : GlassesState()
    data object Paused : GlassesState()
    data class Error(val message: String, val cause: Throwable? = null) : GlassesState()
}
