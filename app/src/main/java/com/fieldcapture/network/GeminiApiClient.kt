/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.network

import android.util.Base64
import android.util.Log
import com.fieldcapture.BuildConfig
import com.fieldcapture.data.local.ApiKeyManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini Vision API client with [ApiKeyManager] integration (user-entered key > BuildConfig keys),
 * automatic multi-key rotation on HTTP 429 rate limits, exponential backoff retries on transient
 * 5xx/network errors, and three-model fallback.
 */
@Singleton
class GeminiApiClient internal constructor(
    private val keyProvider: () -> List<String>,
    private val modelEndpoints: List<String> = DEFAULT_MODEL_ENDPOINTS,
    private val client: OkHttpClient = defaultOkHttpClient()
) {
    /**
     * Primary Hilt constructor pulling keys dynamically from [ApiKeyManager]
     * (user EncryptedSharedPreferences key first, BuildConfig keys second).
     */
    @Inject
    constructor(apiKeyManager: ApiKeyManager) : this(
        keyProvider = { apiKeyManager.getAllAvailableKeys() }
    )

    /**
     * Constructor with static key list (used in unit tests and custom rotation setups).
     */
    constructor(
        apiKeys: List<String>,
        modelEndpoints: List<String> = DEFAULT_MODEL_ENDPOINTS,
        client: OkHttpClient = defaultOkHttpClient()
    ) : this(
        keyProvider = { normalizeKeys(apiKeys) },
        modelEndpoints = modelEndpoints,
        client = client
    )

    /**
     * Fallback constructor loading keys from BuildConfig directly.
     */
    constructor() : this(
        apiKeys = parseKeys(
            BuildConfig.GEMINI_API_KEYS.ifBlank { BuildConfig.GEMINI_API_KEY }
        )
    )

    /**
     * Backward-compatible constructor accepting a single key or comma-separated keys.
     */
    constructor(apiKey: String) : this(apiKeys = parseKeys(apiKey))

    private val gson = Gson()
    private val currentKeyIndex = AtomicInteger(0)

    class AllKeysExhaustedException(message: String) : IOException(message)

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS = longArrayOf(1000L, 2000L, 4000L)

        val DEFAULT_MODEL_ENDPOINTS = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
        )

        fun normalizeKeys(keys: List<String>): List<String> =
            keys.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        fun parseKeys(raw: String): List<String> = normalizeKeys(raw.split(","))

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Number of distinct API keys currently available across user + BuildConfig storage. */
    fun keyCount(): Int = keyProvider().size

    /** Current active key index (wrapped modulo current [keyCount]). */
    fun currentKeyIndex(): Int {
        val count = keyProvider().size
        return if (count == 0) 0 else currentKeyIndex.get() % count
    }

    private fun activeKey(keys: List<String>): String = keys[currentKeyIndex.get() % keys.size]

    private fun advanceKey(keys: List<String>): Int {
        val next = currentKeyIndex.incrementAndGet() % keys.size
        Log.w(TAG, "Rotating API key to index $next (of ${keys.size})")
        return next
    }

    /**
     * Performs a lightweight test request against Gemini using a specific [apiKey].
     */
    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key is empty"))
        }

        val testPayload = gson.toJson(
            mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to "Reply OK")))))
        ).toRequestBody("application/json".toMediaType())

        val endpoint = modelEndpoints.first()
        val request = Request.Builder()
            .url("$endpoint?key=$trimmed")
            .post(testPayload)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> Result.success("Key valid ✓")
                    response.code == 429 -> Result.success("Key valid ✓ (daily free-tier quota currently reached)")
                    response.code == 400 || response.code == 401 || response.code == 403 ->
                        Result.failure(IOException("Key invalid ✗ (HTTP ${response.code})"))
                    else -> Result.failure(IOException("HTTP ${response.code}: ${body.take(120)}"))
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Send an optional image + text prompt to Gemini Vision API and return raw JSON text.
     *
     * - Dynamically fetches keys via [keyProvider] (user Settings key > BuildConfig keys).
     * - On HTTP 429 with multiple keys: rotates immediately to the next key with zero backoff.
     * - On HTTP 5xx or transient [IOException]: retries on the same key with exponential backoff.
     * - On HTTP 404: falls back to the next model endpoint.
     */
    suspend fun analyzeImage(
        imageBytes: ByteArray? = null,
        mimeType: String = "image/jpeg",
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKeys = keyProvider()
        if (apiKeys.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("No Gemini API key configured. Open Settings (gear icon) to enter your key.")
            )
        }

        val parts = buildList<Map<String, Any>> {
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                add(
                    mapOf(
                        "inlineData" to mapOf(
                            "mimeType" to mimeType,
                            "data" to base64Image
                        )
                    )
                )
            }
            add(mapOf("text" to prompt))
        }

        val requestJson = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(mapOf("parts" to parts))))
            add(
                "generationConfig",
                gson.toJsonTree(
                    mapOf(
                        "temperature" to 0.1,
                        "maxOutputTokens" to 2048,
                        "responseMimeType" to "application/json"
                    )
                )
            )
        }

        val requestBody = gson.toJson(requestJson)
            .toRequestBody("application/json".toMediaType())

        var lastException: Exception? = null
        var sawOnly429AcrossAllModels = true

        for (baseUrl in modelEndpoints) {
            var retriesOnSameKey = 0
            var rotationsOnModel = 0

            while (retriesOnSameKey < MAX_RETRIES) {
                val keyIdx = currentKeyIndex.get() % apiKeys.size
                val apiKey = activeKey(apiKeys)
                val url = "$baseUrl?key=$apiKey"

                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string().orEmpty()
                        when {
                            response.isSuccessful -> {
                                val text = extractTextFromResponse(responseBody)
                                return@withContext if (text != null) {
                                    Result.success(text)
                                } else {
                                    Result.failure(IOException("Failed to parse Gemini response: $responseBody"))
                                }
                            }
                            response.code == 429 -> {
                                Log.w(TAG, "429 RESOURCE_EXHAUSTED on key[$keyIdx] ($baseUrl)")
                                lastException = IOException("Rate limited: ${response.code}")
                                if (apiKeys.size > 1 && rotationsOnModel < apiKeys.size - 1) {
                                    rotationsOnModel++
                                    advanceKey(apiKeys)
                                    continue
                                } else if (apiKeys.size == 1 && retriesOnSameKey < MAX_RETRIES - 1) {
                                    delay(RETRY_DELAYS[retriesOnSameKey])
                                    retriesOnSameKey++
                                } else {
                                    if (apiKeys.size > 1) {
                                        advanceKey(apiKeys)
                                    }
                                    break
                                }
                            }
                            response.code in 500..599 -> {
                                sawOnly429AcrossAllModels = false
                                Log.w(TAG, "Server error (${response.code}) on $baseUrl, attempt ${retriesOnSameKey + 1}/$MAX_RETRIES")
                                lastException = IOException("Server error: ${response.code}")
                                if (retriesOnSameKey < MAX_RETRIES - 1) delay(RETRY_DELAYS[retriesOnSameKey])
                                retriesOnSameKey++
                            }
                            response.code == 404 -> {
                                sawOnly429AcrossAllModels = false
                                Log.w(TAG, "Model unavailable (404) at $baseUrl; trying fallback model")
                                lastException = IOException("Model not found (404): $responseBody")
                                break
                            }
                            else -> {
                                return@withContext Result.failure(
                                    IOException("API error ${response.code}: $responseBody")
                                )
                            }
                        }
                    }
                } catch (e: IOException) {
                    sawOnly429AcrossAllModels = false
                    Log.e(TAG, "Network error on $baseUrl, attempt ${retriesOnSameKey + 1}/$MAX_RETRIES", e)
                    lastException = e
                    if (retriesOnSameKey < MAX_RETRIES - 1) delay(RETRY_DELAYS[retriesOnSameKey])
                    retriesOnSameKey++
                }
            }
        }

        if (sawOnly429AcrossAllModels && lastException?.message?.contains("429") == true) {
            return@withContext Result.failure(
                AllKeysExhaustedException(
                    "All ${apiKeys.size} API key(s) hit rate limit (429) across all models. " +
                        "Free tier allows 20 requests/day/key (${apiKeys.size * 20} requests/day total)."
                )
            )
        }

        Result.failure(lastException ?: IOException("Failed after $MAX_RETRIES attempts"))
    }

    private fun extractTextFromResponse(responseBody: String): String? {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response envelope", e)
            null
        }
    }
}
