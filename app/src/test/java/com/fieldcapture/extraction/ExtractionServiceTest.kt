package com.fieldcapture.extraction

import android.app.Application
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.network.GeminiApiClient
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ExtractionServiceTest {

    private val gson = Gson()
    private val extractionService = ExtractionService(GeminiApiClient())

    @Test
    fun `nameplate prompt requests JSON format`() {
        val prompt = ExtractionPrompts.nameplatePrompt()
        assertTrue(prompt.contains("Return ONLY valid JSON"))
        assertTrue(prompt.contains("model_number"))
        assertTrue(prompt.contains("serial_number"))
        assertTrue(prompt.contains("confidence"))
    }

    @Test
    fun `damage prompt includes room context when provided`() {
        val prompt = ExtractionPrompts.damagePrompt(room = "kitchen", surface = "north wall")
        assertTrue(prompt.contains("kitchen"))
        assertTrue(prompt.contains("north wall"))
    }

    @Test
    fun `damage prompt handles null room and surface`() {
        val prompt = ExtractionPrompts.damagePrompt(room = null, surface = null)
        assertTrue(prompt.contains("No room/surface info provided"))
    }

    @Test
    fun `note prompt includes spoken text`() {
        val prompt = ExtractionPrompts.notePrompt("homeowner says leak started Tuesday")
        assertTrue(prompt.contains("homeowner says leak started Tuesday"))
    }

    @Test
    fun `damage prompt lists all line item codes`() {
        val prompt = ExtractionPrompts.damagePrompt(null, null)
        assertTrue(prompt.contains("WATER_EXTRACTION"))
        assertTrue(prompt.contains("REMOVE_REPLACE_DRYWALL"))
        assertTrue(prompt.contains("MOLD_REMEDIATION"))
        assertTrue(prompt.contains("APPLIANCE_REPLACEMENT"))
    }

    @Test
    fun `parse valid nameplate JSON`() {
        val json = """
        {
            "equipment_type": "Water Heater",
            "manufacturer": "Rheem",
            "model_number": "XG40T06EC36U1",
            "serial_number": "MP1234567890",
            "manufacture_date": "2019",
            "capacity": "40 GAL",
            "voltage": "120V",
            "additional_specs": null,
            "confidence": {
                "equipment_type": 0.95,
                "manufacturer": 0.92,
                "model_number": 0.88,
                "serial_number": 0.75,
                "manufacture_date": 0.80,
                "capacity": 0.90
            },
            "overall_confidence": 0.87,
            "notes": "Label slightly faded but readable"
        }
        """.trimIndent()

        val result = gson.fromJson(json, NameplateExtraction::class.java)
        assertEquals("Water Heater", result.equipmentType)
        assertEquals("Rheem", result.manufacturer)
        assertEquals("XG40T06EC36U1", result.modelNumber)
        assertEquals(0.87, result.overallConfidence, 0.01)
    }

    @Test
    fun `parse valid damage JSON with line items`() {
        val json = """
        {
            "damage_type": "water_damage",
            "severity": "high",
            "description": "Water staining on drywall",
            "affected_materials": ["drywall", "baseboard"],
            "affected_area_estimate": "~16 sq ft",
            "room": "kitchen",
            "surface": "north wall",
            "moisture_visible": true,
            "mold_visible": false,
            "recommended_line_items": [
                {
                    "code": "REMOVE_REPLACE_DRYWALL",
                    "reason": "Drywall shows water damage",
                    "priority": "high",
                    "estimated_quantity": "16 sq ft"
                }
            ],
            "confidence": {
                "damage_type": 0.9,
                "severity": 0.8,
                "affected_materials": 0.85,
                "line_items": 0.75
            },
            "overall_confidence": 0.82,
            "safety_concerns": null,
            "notes": "Active moisture present"
        }
        """.trimIndent()

        val result = gson.fromJson(json, DamageExtraction::class.java)
        assertEquals("water_damage", result.damageType)
        assertEquals("high", result.severity)
        assertEquals(1, result.recommendedLineItems.size)
        assertEquals("REMOVE_REPLACE_DRYWALL", result.recommendedLineItems[0].code)
    }

    @Test
    fun `parse nameplate with null fields`() {
        val json = """
        {
            "equipment_type": "Furnace",
            "manufacturer": null,
            "model_number": null,
            "serial_number": null,
            "manufacture_date": null,
            "capacity": "80000 BTU",
            "voltage": null,
            "additional_specs": null,
            "confidence": {
                "equipment_type": 0.9,
                "manufacturer": 0.0,
                "model_number": 0.0,
                "serial_number": 0.0,
                "manufacture_date": 0.0,
                "capacity": 0.7
            },
            "overall_confidence": 0.3,
            "notes": "Label badly damaged, most text unreadable"
        }
        """.trimIndent()

        val result = gson.fromJson(json, NameplateExtraction::class.java)
        assertEquals("Furnace", result.equipmentType)
        assertNull(result.manufacturer)
        assertNull(result.modelNumber)
        assertEquals(0.3, result.overallConfidence, 0.01)
    }

    @Test
    fun `parseResponse strips markdown fences and extra surrounding text`() {
        val wrapped = """
            Here is the extracted JSON:
            ```json
            {
              "transcribed_note": "Subfloor is soft near the kitchen cabinet.",
              "category": "observation",
              "image_context": "Kitchen floor near sink base",
              "key_facts": ["soft subfloor", "near cabinet"],
              "confidence": { "transcription": 0.95, "category": 0.9, "image_context": 0.85 },
              "overall_confidence": 0.9,
              "follow_up_needed": false,
              "follow_up_reason": null
            }
            ```
        """.trimIndent()

        val result = extractionService.parseResponse(wrapped, CaptureType.NOTE) as NoteExtraction
        assertEquals("observation", result.category)
        assertEquals(0.9, result.overallConfidence, 0.01)
        assertEquals(2, result.keyFacts.size)
        assertNull(result.error)
    }

    @Test
    fun `parseResponse returns safe error result on malformed JSON`() {
        val malformed = "{ not valid json"
        val result = extractionService.parseResponse(malformed, CaptureType.NAMEPLATE) as NameplateExtraction
        assertEquals("parse_error", result.error)
        assertEquals(0.0, result.overallConfidence, 0.001)
    }
}
