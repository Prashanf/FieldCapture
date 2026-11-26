package com.fieldcapture.data.local

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ApiKeyManagerTest {

    private fun createManager(
        buildConfigKeys: List<String> = emptyList(),
        prefsName: String = "test_prefs_${System.nanoTime()}"
    ): ApiKeyManager {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return ApiKeyManager(prefs = prefs, buildConfigKeysProvider = { buildConfigKeys })
    }

    @Test
    fun `no keys returns null and empty rotation list`() {
        val manager = createManager(buildConfigKeys = emptyList())
        assertNull(manager.getActiveKey())
        assertFalse(manager.hasUserKey())
        assertFalse(manager.hasAnyKey())
        assertNull(manager.getMaskedActiveKey())
        assertTrue(manager.getAllAvailableKeys().isEmpty())
    }

    @Test
    fun `falls back to BuildConfig keys when no user key is saved`() {
        val manager = createManager(buildConfigKeys = listOf("dev_key_1", "dev_key_2"))
        assertEquals("dev_key_1", manager.getActiveKey())
        assertFalse(manager.hasUserKey())
        assertTrue(manager.hasAnyKey())
        assertEquals(listOf("dev_key_1", "dev_key_2"), manager.getAllAvailableKeys())
    }

    @Test
    fun `user key takes highest priority over BuildConfig fallback`() {
        val manager = createManager(buildConfigKeys = listOf("dev_key_1", "dev_key_2"))
        manager.saveUserKey("  user_secret_ab1c  ")

        assertTrue(manager.hasUserKey())
        assertEquals("user_secret_ab1c", manager.getActiveKey())
        assertEquals("user_secret_ab1c", manager.activeKeyFlow.value)
        assertEquals(listOf("user_secret_ab1c", "dev_key_1", "dev_key_2"), manager.getAllAvailableKeys())
    }

    @Test
    fun `clearUserKey removes stored key and reverts to BuildConfig fallback`() {
        val manager = createManager(buildConfigKeys = listOf("dev_key_1"))
        manager.saveUserKey("user_key_xyz9")
        assertTrue(manager.hasUserKey())

        manager.clearUserKey()
        assertFalse(manager.hasUserKey())
        assertEquals("dev_key_1", manager.getActiveKey())
        assertEquals(listOf("dev_key_1"), manager.getAllAvailableKeys())
    }

    @Test
    fun `maskKey exposes only last four characters`() {
        assertEquals("••••••••ab1c", ApiKeyManager.maskKey("AQ.test_key_123456ab1c"))
        assertEquals("••••", ApiKeyManager.maskKey("abc"))
        assertNull(ApiKeyManager.maskKey("   "))
    }
}
