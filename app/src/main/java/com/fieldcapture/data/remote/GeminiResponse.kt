package com.fieldcapture.data.remote

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiApiError? = null
) {
    fun primaryText(): String? =
        candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
}

data class GeminiCandidate(
    val content: GeminiCandidateContent? = null
)

data class GeminiCandidateContent(
    val parts: List<GeminiResponsePart>? = null
)

data class GeminiResponsePart(
    val text: String? = null
)

data class GeminiApiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
