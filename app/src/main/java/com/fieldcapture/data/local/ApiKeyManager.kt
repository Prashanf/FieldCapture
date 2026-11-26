/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fieldcapture.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Gemini API keys securely using AndroidX Security Crypto ([EncryptedSharedPreferences] AES-256-GCM).
 *
 * Key priority order:
 * 1. User-entered key in EncryptedSharedPreferences (highest priority)
 * 2. BuildConfig keys from local.properties (developer fallback)
 * 3. No key available -> UI prompts user to enter one in Settings
 */
@Singleton
class ApiKeyManager @VisibleForTesting internal constructor(
    private val prefs: SharedPreferences,
    private val buildConfigKeysProvider: () -> List<String>
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        prefs = createSecurePrefs(context),
        buildConfigKeysProvider = { defaultBuildConfigKeys() }
    )

    private val _activeKeyFlow = MutableStateFlow(computeActiveKey())
    val activeKeyFlow: StateFlow<String?> = _activeKeyFlow.asStateFlow()

    /** Returns highest-priority active key, or null if none configured. */
    fun getActiveKey(): String? = computeActiveKey()

    /** Saves a user-entered Gemini API key into EncryptedSharedPreferences. */
    fun saveUserKey(key: String) {
        val trimmed = key.trim()
        require(trimmed.isNotBlank()) { "API key cannot be blank" }
        prefs.edit().putString(KEY_USER_GEMINI_API, trimmed).apply()
        _activeKeyFlow.value = computeActiveKey()
    }

    /** Removes the user-entered API key, reverting to BuildConfig fallback if present. */
    fun clearUserKey() {
        prefs.edit().remove(KEY_USER_GEMINI_API).apply()
        _activeKeyFlow.value = computeActiveKey()
    }

    /** True if a non-blank user-entered key is saved in secure storage. */
    fun hasUserKey(): Boolean = !readUserKey().isNullOrBlank()

    /** Returns true if either a user key or BuildConfig key is configured. */
    fun hasAnyKey(): Boolean = getActiveKey() != null

    /** Masks key leaving only the last 4 characters visible (e.g., "••••••••ab1c"). */
    fun getMaskedActiveKey(): String? = maskKey(getActiveKey())

    /**
     * Returns all available API keys ordered for rotation:
     * user-entered key first (highest priority), followed by distinct BuildConfig keys.
     */
    fun getAllAvailableKeys(): List<String> {
        val userKey = readUserKey()
        val devKeys = buildConfigKeysProvider()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return buildList {
            if (!userKey.isNullOrBlank()) add(userKey)
            addAll(devKeys)
        }.distinct()
    }

    private fun readUserKey(): String? =
        prefs.getString(KEY_USER_GEMINI_API, null)?.trim()?.takeIf { it.isNotBlank() }

    private fun computeActiveKey(): String? =
        readUserKey() ?: buildConfigKeysProvider().firstOrNull { it.isNotBlank() }

    companion object {
        private const val TAG = "ApiKeyManager"
        private const val PREFS_FILE_NAME = "fieldcapture_secure_prefs"
        private const val ROBOLECTRIC_FALLBACK_PREFS = "fieldcapture_test_prefs"
        private const val KEY_USER_GEMINI_API = "user_gemini_api_key"

        fun maskKey(key: String?): String? {
            if (key.isNullOrBlank()) return null
            val trimmed = key.trim()
            return if (trimmed.length <= 4) {
                "••••"
            } else {
                "••••••••${trimmed.takeLast(4)}"
            }
        }

        fun defaultBuildConfigKeys(): List<String> {
            val raw = BuildConfig.GEMINI_API_KEYS.ifBlank { BuildConfig.GEMINI_API_KEY }
            return raw.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        @Suppress("DEPRECATION")
        private fun createSecurePrefs(context: Context): SharedPreferences {
            if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) {
                return context.getSharedPreferences(ROBOLECTRIC_FALLBACK_PREFS, Context.MODE_PRIVATE)
            }
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create EncryptedSharedPreferences; falling back to private prefs", e)
                context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
            }
        }
    }
}
