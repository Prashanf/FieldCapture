package com.fieldcapture.glasses

import android.content.Context
import android.net.Uri
import android.util.Log
import com.meta.wearable.dat.mockdevice.MockDeviceKit
import com.meta.wearable.dat.mockdevice.api.GlassesModel
import com.meta.wearable.dat.mockdevice.api.MockDeviceKitInterface
import com.meta.wearable.dat.mockdevice.api.MockGlasses
import com.meta.wearable.dat.mockdevice.api.camera.CameraFacing
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mockDeviceKit: MockDeviceKitInterface? = null
    private var pairedGlasses: MockGlasses? = null

    fun enableMockDevice() {
        val mdk = MockDeviceKit.getInstance(context)
        if (!mdk.isEnabled) {
            mdk.enable()
            Log.d(TAG, "MockDeviceKit enabled")
        }
        mockDeviceKit = mdk
    }

    suspend fun pairAndSetup(cameraFacing: CameraFacing = CameraFacing.BACK): Result<Unit> {
        val mdk = mockDeviceKit ?: return Result.failure(IllegalStateException("MockDeviceKit not enabled"))

        return try {
            val device = pairedGlasses ?: mdk.pairGlasses(GlassesModel.RAYBAN_META).getOrThrow().also {
                pairedGlasses = it
            }

            device.powerOn()
            device.don()
            device.unfold()
            device.services.camera.setCameraFeed(cameraFacing)
            Log.d(TAG, "Mock Ray-Ban Meta glasses paired and ready (feed=$cameraFacing)")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup mock glasses", e)
            Result.failure(e)
        }
    }

    fun setMockCameraFeedUri(videoUri: Uri): Result<Unit> = runCatching {
        val glasses = pairedGlasses ?: error("No paired mock glasses")
        glasses.services.camera.setCameraFeed(videoUri)
    }

    fun setMockCapturedImageUri(imageUri: Uri): Result<Unit> = runCatching {
        val glasses = pairedGlasses ?: error("No paired mock glasses")
        glasses.services.camera.setCapturedImage(imageUri)
    }

    fun isConfigured(): Boolean = mockDeviceKit?.isEnabled == true && pairedGlasses != null

    fun disable() {
        pairedGlasses?.let { device ->
            runCatching { mockDeviceKit?.unpairDevice(device) }
        }
        pairedGlasses = null
        mockDeviceKit?.disable()
        mockDeviceKit = null
        Log.d(TAG, "MockDeviceKit disabled")
    }

    companion object {
        private const val TAG = "MockDeviceManager"
    }
}
