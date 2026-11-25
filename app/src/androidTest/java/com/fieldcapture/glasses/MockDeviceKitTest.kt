package com.fieldcapture.glasses

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockDeviceKitTest {

    @Test
    fun mockDeviceManagerInitializesAndPairsCleanly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockDeviceManager = MockDeviceManager(context)
        mockDeviceManager.enableMockDevice()
        val result = mockDeviceManager.pairAndSetup()
        assertTrue(result.isSuccess)
        assertTrue(mockDeviceManager.isConfigured())
        mockDeviceManager.disable()
    }
}
