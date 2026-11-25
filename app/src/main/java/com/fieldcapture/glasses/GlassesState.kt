package com.fieldcapture.glasses

sealed class GlassesState {
    data object Disconnected : GlassesState()
    data object Connecting : GlassesState()
    data object Connected : GlassesState()
    data object Paused : GlassesState()
    data class Error(val message: String) : GlassesState()
}
