package com.fieldcapture.glasses

import android.graphics.Bitmap

interface GlassesEventListener {
    fun onStateChanged(state: GlassesState)
    fun onFrameAvailable(bitmap: Bitmap)
    fun onError(message: String, throwable: Throwable? = null)
}
