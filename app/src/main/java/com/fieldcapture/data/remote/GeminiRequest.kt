package com.fieldcapture.data.remote

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = GeminiGenerationConfig()
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    @SerializedName("inline_data")
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mime_type")
    val mimeType: String = "image/jpeg",
    val data: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.1,
    @SerializedName("response_mime_type")
    val responseMimeType: String = "application/json"
)
