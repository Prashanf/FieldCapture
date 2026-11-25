package com.fieldcapture.util

object Constants {
    // TODO: Load Gemini API key securely via BuildConfig / local.properties
    const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    const val GEMINI_MODEL = "gemini-1.5-flash"

    const val DEFAULT_DEVICE_NAME = "mock_rayban_meta_gen2"
    const val DEFAULT_TECHNICIAN_ID = "tech_001"

    val NAMEPLATE_PROMPT = """
        Analyze this appliance nameplate image. Extract the following fields as JSON:
        - make (manufacturer/brand name)
        - model (model number)
        - serial_number
        - manufacture_date (in ISO format if possible)
        For each field, provide a "value" and a "confidence" score from 0.0 to 1.0.
        If a field is not visible or unreadable, set value to null and confidence to 0.0.
        Respond ONLY with valid JSON, no markdown.
    """.trimIndent()

    val DAMAGE_PROMPT = """
        Analyze this image of property damage. Extract the following fields as JSON:
        - room (which room this appears to be in)
        - surface (wall/ceiling/floor/baseboard/etc)
        - damage_type (water stain/mold/structural/discoloration/peeling/buckling/etc)
        - estimated_size_sqft (rough estimate of damaged area in square feet)
        - severity (minor/moderate/severe)
        For each field, provide a "value" and a "confidence" score from 0.0 to 1.0.
        Respond ONLY with valid JSON, no markdown.
    """.trimIndent()
}
