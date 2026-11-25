package com.fieldcapture.extraction

object ExtractionPrompts {

    fun nameplatePrompt(): String = """
You are an expert field technician analyzing an equipment nameplate photo. Extract all readable information into structured JSON.

IMPORTANT RULES:
- Extract ONLY what you can actually read in the image
- For any field you cannot read clearly, set the value to null
- DO NOT GUESS serial numbers or model numbers — if partially readable, include only the characters you're confident about and use "?" for unclear characters (e.g., "MP12??567890")
- Confidence scores: 1.0 = completely certain, 0.7+ = mostly readable, 0.4-0.6 = partially readable, below 0.4 = unreliable guess
- If this is NOT a nameplate/equipment label, return {"error": "not_a_nameplate", "description": "brief description of what the image shows"}

Return ONLY valid JSON in this exact format:
{
  "equipment_type": "string or null (e.g., 'Water Heater', 'Furnace', 'AC Unit', 'Washer')",
  "manufacturer": "string or null (e.g., 'Rheem', 'Carrier', 'Lennox')",
  "model_number": "string or null (exact as printed, use ? for unclear characters)",
  "serial_number": "string or null (exact as printed, use ? for unclear characters)",
  "manufacture_date": "string or null (year or full date if visible)",
  "capacity": "string or null (e.g., '40 GAL', '80,000 BTU', '3.5 TON')",
  "voltage": "string or null (e.g., '120V', '240V')",
  "additional_specs": "string or null (any other readable specs)",
  "confidence": {
    "equipment_type": 0.0,
    "manufacturer": 0.0,
    "model_number": 0.0,
    "serial_number": 0.0,
    "manufacture_date": 0.0,
    "capacity": 0.0
  },
  "overall_confidence": 0.0,
  "notes": "string — any observations about readability, label condition, etc."
}
""".trimIndent()

    fun damagePrompt(room: String?, surface: String?): String {
        val locationContext = buildString {
            if (!room.isNullOrBlank()) append("The technician reported this is in the: $room. ")
            if (!surface.isNullOrBlank()) append("The affected surface is: $surface. ")
            if (room.isNullOrBlank() && surface.isNullOrBlank()) {
                append("No room/surface info provided by the technician. ")
            }
        }

        return """
You are an expert restoration/insurance field technician analyzing a damage photo. Identify the damage and recommend repair line items.

LOCATION CONTEXT: $locationContext

SEVERITY SCALE:
- "low": Cosmetic damage only, minor staining, small affected area (<2 sq ft)
- "medium": Moderate damage requiring repair, visible deterioration, 2-20 sq ft
- "high": Significant structural or material damage, requires major repair, 20-100 sq ft
- "critical": Safety hazard, structural compromise, extensive damage, >100 sq ft or requires immediate action

AVAILABLE LINE ITEM CODES (use ONLY these):
- WATER_EXTRACTION: Standing water or saturated materials need water removal
- REMOVE_REPLACE_DRYWALL: Drywall is damaged, stained, warped, or has mold
- REMOVE_REPLACE_INSULATION: Insulation is wet, contaminated, or damaged
- MOLD_REMEDIATION: Visible mold growth or conditions likely to cause mold
- PAINT_PRIME_WALLS: Walls need repainting after repair
- REMOVE_REPLACE_FLOORING: Floor material (carpet, hardwood, tile, vinyl) is damaged
- REMOVE_REPLACE_BASEBOARD: Baseboards are warped, stained, or damaged
- STRUCTURAL_DRY_OUT: Materials need drying (not standing water — that's WATER_EXTRACTION)
- DEHUMIDIFIER_RENTAL: Humidity control needed during drying
- AIR_MOVER_RENTAL: Air circulation needed during drying
- ANTIMICROBIAL_TREATMENT: Disinfection needed to prevent mold/bacteria
- CONTENT_MANIPULATION: Furniture/belongings need moving to access damage
- CEILING_REPAIR: Ceiling is stained, sagging, or damaged
- PLUMBING_REPAIR: Visible plumbing issue (leaking pipe, failed fitting)
- APPLIANCE_REPLACEMENT: Appliance is damaged beyond repair

IMPORTANT RULES:
- Only recommend line items you can justify from what's visible in the image
- Each line item needs a reason explaining why it's needed
- If this is NOT a damage photo, return {"error": "not_damage", "description": "brief description"}
- Confidence reflects how certain you are about the damage assessment

Return ONLY valid JSON in this exact format:
{
  "damage_type": "water_damage | fire_damage | mold | structural | wind_damage | vandalism | other",
  "severity": "low | medium | high | critical",
  "description": "2-3 sentence description of visible damage",
  "affected_materials": ["drywall", "carpet", "baseboard", "etc"],
  "affected_area_estimate": "string (e.g., 'approximately 8 sq ft', 'entire north wall ~40 sq ft')",
  "room": "string or null (from context or visible in image)",
  "surface": "string or null (wall, floor, ceiling, etc.)",
  "moisture_visible": true,
  "mold_visible": false,
  "recommended_line_items": [
    {
      "code": "REMOVE_REPLACE_DRYWALL",
      "reason": "Drywall shows water staining and buckling along lower 2 feet",
      "priority": "high | medium | low",
      "estimated_quantity": "string or null (e.g., '~16 sq ft', '1 unit')"
    }
  ],
  "confidence": {
    "damage_type": 0.0,
    "severity": 0.0,
    "affected_materials": 0.0,
    "line_items": 0.0
  },
  "overall_confidence": 0.0,
  "safety_concerns": "string or null (e.g., 'possible electrical hazard from water near outlet')",
  "notes": "string — additional observations"
}
""".trimIndent()
    }

    fun notePrompt(spokenText: String): String = """
You are helping a field technician document observations. They spoke the following note while taking this photo:

SPOKEN NOTE: "$spokenText"

Analyze the image alongside the spoken note to create a structured record.

CATEGORIES:
- "observation": Tech's visual observation about conditions
- "measurement": Any measurement or reading noted
- "homeowner_statement": Information reported by the property owner
- "safety_concern": Anything related to safety hazards
- "timeline": Information about when damage/events occurred
- "general": Anything that doesn't fit above categories

Return ONLY valid JSON in this exact format:
{
  "transcribed_note": "cleaned up version of the spoken note (fix grammar, expand abbreviations)",
  "category": "observation | measurement | homeowner_statement | safety_concern | timeline | general",
  "image_context": "brief description of what the image shows, relevant to the note",
  "key_facts": ["list", "of", "important", "facts", "from", "note", "and", "image"],
  "confidence": {
    "transcription": 0.0,
    "category": 0.0,
    "image_context": 0.0
  },
  "overall_confidence": 0.0,
  "follow_up_needed": true,
  "follow_up_reason": "string or null (e.g., 'measurement should be verified')"
}
""".trimIndent()
}
